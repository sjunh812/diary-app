// Figma "화면" 페이지 규격 검사. figma_execute에 그대로 넣어 돌린다(읽기만 한다).
// 화면을 고친 뒤에 돌려서 나온 곳을 고친다. 눈으로 보는 점검을 대신하지는 않는다.
// 잡는 것: 칩 좌우 안쪽, 손으로 찍은 부품, 규격에 없는 글자·모서리·안쪽 여백, 변수를 안 쓴 색, 라이트·다크가 뒤바뀐 부품.
if (figma.root.name !== '1일 1일기') throw new Error('wrong file');
await figma.loadAllPagesAsync();
figma.skipInvisibleInstanceChildren = true;
const page = figma.root.children.find((p) => p.name === '화면');
const spacing = new Set([0, 1, 2, 3, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 34, 36, 40, 56, 70]);
const radii = new Set([0, 8, 10, 12, 14, 16, 20, 28, 999]);
const types = new Set(['28/38/Bold', '22/30/Bold', '20/28/Bold', '22/28/Medium', '28/34/Regular', '17/24/SemiBold', '17/28/Regular',
  '15/22/Regular', '15/22/SemiBold', '14/20/Medium', '13/18/Regular', '13/18/Medium', '12/16/Medium']);
const skip = (n) => {
  for (let p = n; p && p.type !== 'PAGE'; p = p.parent) {
    if (/설명|로띠|보관|^mark\/|^status|^시스템/.test(p.name)) return true;
  }
  return false;
};
const path = (n) => {
  const names = [];
  for (let p = n; p && p.type !== 'PAGE' && names.length < 3; p = p.parent) names.push(p.name.replace(/⁠/g, '').slice(0, 14));
  return names.join(' < ');
};
const dark = (n) => {
  for (let p = n; p && p.type !== 'PAGE'; p = p.parent) if (p.name.includes('다크')) return true;
  return false;
};
const found = {};
const report = (kind, n, value) => {
  found[kind] = found[kind] || { count: 0, examples: [] };
  found[kind].count += 1;
  if (found[kind].examples.length < 5) found[kind].examples.push(`${n.id} ${path(n)} ${value}`);
};
for (const n of page.findAll((x) => x.visible !== false && !skip(x))) {
  if (n.type === 'INSTANCE') {
    const main = await n.getMainComponentAsync();
    const set = main && main.parent && main.parent.type === 'COMPONENT_SET' ? main.parent.name : '';
    if (set === '칩') {
      const first = n.children.find((c) => c.visible);
      const left = first && first.type === 'TEXT' ? 12 : 10;
      if (n.paddingLeft !== left || n.paddingRight !== 12 || Math.round(n.height) !== 32) report('칩 규격', n, `${n.paddingLeft}/${n.paddingRight}`);
    }
    if (main && main.name.includes('테마=') && main.name.includes('다크') !== dark(n)) report('테마가 다른 부품', n, main.name);
  }
  if (n.type === 'FRAME' && !n.id.startsWith('I') && /^(chip|button\/|btn|toggle|toast)/i.test(n.name)) report('손으로 찍은 부품', n, n.name);
  if (n.type === 'TEXT' && n.fontName !== figma.mixed && n.fontSize !== figma.mixed) {
    const key = `${n.fontSize}/${n.lineHeight.value || 'auto'}/${n.fontName.style}`;
    if (!types.has(key)) report('글자 규격', n, key);
    if (!n.textStyleId) report('글자 스타일 없음', n, key);
  }
  if ('layoutMode' in n && n.layoutMode !== 'NONE') {
    for (const v of [n.paddingLeft, n.paddingRight, n.paddingTop, n.paddingBottom]) if (!spacing.has(v)) report('안쪽 여백', n, v);
  }
  if (n.type === 'FRAME' && n.cornerRadius !== figma.mixed && !radii.has(n.cornerRadius) && n.cornerRadius < Math.min(n.width, n.height) / 2) {
    report('모서리', n, n.cornerRadius);
  }
  if ('fills' in n && Array.isArray(n.fills) && !n.id.startsWith('I')) {
    for (const f of n.fills) if (f.type === 'SOLID' && f.visible !== false && !f.boundVariables?.color) report('변수 없는 색', n, n.type);
  }
}
return found;
