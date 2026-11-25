//! Allocation-conscious app search and personalization scoring for the launcher.
//! Android rendering, package discovery, preferences, and lifecycle stay native Kotlin.

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use std::panic::{catch_unwind, AssertUnwindSafe};

#[derive(Debug)]
struct Candidate<'a> {
    package: &'a str,
    label: &'a str,
    score: i32,
    position: usize,
}

fn search_score(label: &str, query: &str) -> i32 {
    let query = query.trim();
    if query.is_empty() { return 0; }
    if label.eq_ignore_ascii_case(query) { return 1_000; }
    if label.get(..query.len()).is_some_and(|prefix| prefix.eq_ignore_ascii_case(query)) {
        return 800 - label.len().saturating_sub(query.len()).min(100) as i32;
    }
    if label.split_whitespace().any(|word| word.get(..query.len())
        .is_some_and(|prefix| prefix.eq_ignore_ascii_case(query))) { return 650; }
    if label.to_lowercase().contains(&query.to_lowercase()) { return 500; }

    // A short ordered-subsequence score helps imperfect voice/remote searches.
    let mut remaining = query.chars().flat_map(char::to_lowercase);
    let mut next = remaining.next();
    let mut gaps = 0usize;
    for character in label.chars().flat_map(char::to_lowercase) {
        if next == Some(character) {
            next = remaining.next();
            if next.is_none() { return 220 - gaps.min(100) as i32; }
        } else if next.is_some() {
            gaps += 1;
        }
    }
    -1
}

fn rank(input: &str, query: &str, favorites: &str, recent: &str) -> String {
    let favorite_rank: Vec<&str> = favorites.lines().filter(|value| !value.is_empty()).collect();
    let recent_rank: Vec<&str> = recent.lines().filter(|value| !value.is_empty()).collect();
    let searching = !query.trim().is_empty();
    let mut candidates: Vec<Candidate<'_>> = input.lines().enumerate().filter_map(|(position, line)| {
        let (package, label) = line.split_once('\t')?;
        if package.is_empty() { return None; }
        let text_score = search_score(label, query);
        if searching && text_score < 0 { return None; }
        let favorite = favorite_rank.iter().position(|value| *value == package);
        let recent = recent_rank.iter().position(|value| *value == package);
        Some(Candidate {
            package,
            label,
            score: text_score
                + favorite.map_or(0, |index| 100 - index.min(99) as i32)
                + recent.map_or(0, |index| 50 - index.min(49) as i32),
            position,
        })
    }).collect();

    candidates.sort_unstable_by(|left, right| right.score.cmp(&left.score).then_with(|| {
        if searching { left.label.to_lowercase().cmp(&right.label.to_lowercase()) }
        else { left.position.cmp(&right.position) }
    }));
    candidates.into_iter().map(|candidate| candidate.package).collect::<Vec<_>>().join("\n")
}

#[no_mangle]
pub extern "system" fn Java_com_bluelabs_bluelauncher_nativeengine_NativeEngine_rankApps(
    mut env: JNIEnv,
    _class: JClass,
    apps: JString,
    query: JString,
    favorites: JString,
    recent: JString,
) -> jstring {
    match catch_unwind(AssertUnwindSafe(|| {
        let apps: String = env.get_string(&apps).ok()?.into();
        let query: String = env.get_string(&query).ok()?.into();
        let favorites: String = env.get_string(&favorites).ok()?.into();
        let recent: String = env.get_string(&recent).ok()?.into();
        env.new_string(rank(&apps, &query, &favorites, &recent)).ok().map(|value| value.into_raw())
    })) {
        Ok(Some(result)) => result,
        _ => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::rank;

    #[test]
    fn search_and_personalization() {
        let result = rank(
            "zeta.app\tZeta Player\nalpha.app\tAlpha Video\nbeta.app\tBlue Music",
            "blu", "alpha.app", "beta.app",
        );
        assert_eq!(result, "beta.app");
    }

    #[test]
    fn empty_search_keeps_ranked_favorites() {
        let result = rank("zeta\tZeta\nalpha\tAlpha\nbeta\tBeta", "", "alpha", "beta");
        assert_eq!(result, "alpha\nbeta\nzeta");
    }

    #[test]
    fn unicode_label_prefix_is_safe() {
        let result = rank("app\t東京電車", "東京", "", "");
        assert_eq!(result, "app");
    }
}
