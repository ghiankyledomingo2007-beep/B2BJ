// Paste into the gallery's browser console after its images and font load.
// Read-only checks; this file is not included by index.html.
(() => {
  const assert = (condition, message) => { if (!condition) throw new Error(message); };
  const scenes = [...document.querySelectorAll('#gallery .scene')];
  assert(scenes.length === 5, 'Expected five menu concepts');
  assert([...document.images].every(image => image.complete && image.naturalWidth > 0), 'Image failed to load');
  assert(document.documentElement.scrollWidth <= innerWidth, 'Horizontal page overflow');
  for (const scene of scenes) {
    const bounds = scene.getBoundingClientRect();
    const brand = scene.querySelector('.brand').getBoundingClientRect();
    const nav = scene.querySelector('.game-nav').getBoundingClientRect();
    const footer = scene.querySelector('.scene-footer').getBoundingClientRect();
    assert(brand.bottom <= nav.top, `${scene.className}: logo overlaps navigation`);
    assert(nav.bottom <= footer.top, `${scene.className}: navigation overlaps footer`);
    assert(nav.left >= bounds.left && nav.right <= bounds.right, `${scene.className}: navigation escapes scene`);
    assert(scene.querySelectorAll('.menu-item').length === 4, 'Missing menu action');
    assert(scene.querySelectorAll('.selected').length === 1, 'Expected exactly one selected action');
  }
  if (document.body.classList.contains('paused')) {
    for (const effect of document.querySelectorAll('.scene .world, .scene .rain i')) {
      assert(getComputedStyle(effect).animationPlayState === 'paused', 'Animation did not pause');
    }
  }
  return { passed: true, concepts: scenes.length, viewport: [innerWidth, innerHeight] };
})();
