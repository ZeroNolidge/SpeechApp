// The map layout check (MapLayoutTest) lays out every stop on several screen sizes: give each
// test more than mocha's default 2 seconds.
config.set({ client: { mocha: { timeout: 30000 } } });
