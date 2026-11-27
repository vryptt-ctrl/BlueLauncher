/** Home page copy: a short fact strip and six user-facing features. */
export type Feature = {
  icon: string;
  title: string;
  body: string;
};

export const HIGHLIGHTS = [
  {
    icon: 'offline',
    stat: 'Offline',
    label: 'No network permission',
    body: 'No ads, no analytics, no crash reporting. Nothing leaves the device.',
  },
  {
    icon: 'feather',
    stat: '~1.2 MB',
    label: 'Release APK',
    body: 'Plain platform views and no UI framework keep the install tiny.',
  },
  {
    icon: 'speed',
    stat: '~160 ms',
    label: 'UI ready',
    body: 'Measured on modest hardware, so it stays responsive on low-end boxes.',
  },
  {
    icon: 'code',
    stat: 'GPL-3.0',
    label: 'Open source',
    body: 'Every release APK is built from the public source on GitHub.',
  },
] as const;

export const FEATURES: Feature[] = [
  {
    icon: 'apps',
    title: 'All your apps in one grid',
    body: 'TV apps and regular launcher apps appear together, and the list refreshes when you install or update something.',
  },
  {
    icon: 'pin',
    title: 'Favorites in your order',
    body: 'Hold OK on an app to pin it, then move it where you want. The order is kept across reinstalls.',
  },
  {
    icon: 'search',
    title: 'Fast search',
    body: 'Type part of a name and the best match comes first, with favorites and recent apps ranked higher.',
  },
  {
    icon: 'history',
    title: 'Recents and resume',
    body: 'The last twelve apps you opened stay one click away, and the hero card resumes the one you used last.',
  },
  {
    icon: 'palette',
    title: 'Themes and wallpaper',
    body: 'Three dark themes, or import your own image as the wallpaper. No storage permission needed.',
  },
  {
    icon: 'tune',
    title: 'Display options',
    body: 'Choose compact, comfortable or large cards, hide labels, and show or hide the clock and date.',
  },
];
