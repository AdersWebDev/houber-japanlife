/*
 * 하우버 관리자 글쓰기 편집기
 * - 블로그처럼 자유롭게 쓰고 원하는 자리에 사진을 넣는다 (contenteditable)
 * - 사진은 브라우저에서 화면용 크기로 줄인 뒤 /admin/api/image 로 올린다 (서버 메모리 절약)
 * - 저장할 때는 사이트 상세 페이지 형식(detail.css)으로 바꿔 보낸다. 서버가 한 번 더 안전하게 거른다
 * - 가져오기: HTML 파일 / zip(HTML + 사진 폴더) / 하우버 네이버 블로그 주소
 */
'use strict';

const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];
const token = $('meta[name="admin-token"]').content;
const postData = $('#post-data').dataset;
const postId = postData.id || null;
const editor = $('#editor');
const titleEl = $('#title');
const htmlView = $('#html-view');
const blobs = new Map();          // 올린 사진 주소 -> 줄인 사진 (대표 사진 자를 때 다시 받지 않으려고)
let savedRange = null;
let htmlMode = false;
let dirty = false;
let pendingThumb = null;          // 가져오기에서 받은 대표 사진 후보

document.execCommand('defaultParagraphSeparator', false, 'p');

/* ---------- 공통 ---------- */
function toast(msg, ms = 2600) {
  const t = $('#toast'); t.textContent = msg; t.style.display = 'block';
  clearTimeout(toast.timer); toast.timer = setTimeout(() => (t.style.display = 'none'), ms);
}
async function api(method, url, body) {
  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json', 'X-Admin-Token': token }, body: body ? JSON.stringify(body) : undefined });
  return handle(res);
}
async function handle(res) {
  if (res.status === 401) { alert('로그인이 끊겼습니다. 쓰던 글은 이 브라우저에 자동 저장돼 있으니, 다시 로그인한 뒤 불러오세요.'); location.href = '/admin/login'; throw new Error('login'); }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || ('오류가 났습니다 (' + res.status + ')'));
  return data;
}
function esc(s) { return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function mb(bytes) { return (bytes / 1024 / 1024).toFixed(1) + 'MB'; }
function openModal(id) { $(id).hidden = false; }
function closeModal(el) { el.closest('.modal-bg').hidden = true; }
$$('[data-close]').forEach(b => b.addEventListener('click', () => closeModal(b)));

/* ---------- 사진 줄이기·올리기 ---------- */
async function decodeImage(blob) {
  try { return await createImageBitmap(blob, { imageOrientation: 'from-image' }); }
  catch (e) {
    const url = URL.createObjectURL(blob);
    try {
      return await new Promise((ok, fail) => { const i = new Image(); i.onload = () => ok(i); i.onerror = () => fail(new Error('사진을 열 수 없습니다')); i.src = url; });
    } finally { setTimeout(() => URL.revokeObjectURL(url), 1000); }
  }
}
function canvasBlob(canvas, type, q) { return new Promise(ok => canvas.toBlob(ok, type, q)); }

/** 화면용 크기(긴 변 1600px)로 줄이고 1MB 안쪽 JPEG로. GIF는 움직임 때문에 그대로 */
async function resizeImage(blob, maxSide = 1600) {
  if (blob.type === 'image/gif') return blob;
  const img = await decodeImage(blob);
  const w0 = img.width, h0 = img.height;
  const scale = Math.min(1, maxSide / Math.max(w0, h0));
  if (scale === 1 && blob.size < 400 * 1024 && /jpeg|webp/.test(blob.type)) return blob;
  const c = document.createElement('canvas');
  c.width = Math.round(w0 * scale); c.height = Math.round(h0 * scale);
  const g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height); g.drawImage(img, 0, 0, c.width, c.height);
  let q = 0.85, out = await canvasBlob(c, 'image/jpeg', q);
  while (out && out.size > 900 * 1024 && q > 0.5) { q -= 0.1; out = await canvasBlob(c, 'image/jpeg', q); }
  if (!out) throw new Error('사진을 줄이지 못했습니다');
  // 이미 작은 사진은 다시 압축하면 오히려 커질 수 있어 원본을 씀
  if (scale === 1 && out.size >= blob.size && /jpeg|webp|png/.test(blob.type) && blob.size < 1024 * 1024) return blob;
  return out;
}

async function uploadBlob(blob, alt) {
  const fd = new FormData();
  fd.append('file', blob, blob.type === 'image/gif' ? 'image.gif' : 'image.jpg');
  fd.append('alt', alt || '');
  const res = await fetch('/admin/api/image', { method: 'POST', headers: { 'X-Admin-Token': token }, body: fd });
  const data = await handle(res);
  blobs.set(data.url, blob);
  return data;
}

/** 대표 사진: 가운데를 정사각형으로 잘라 800px로 올림 */
async function makeSquareThumb(url) {
  let blob = blobs.get(url);
  if (!blob) {
    const r = await fetch('/admin/api/fetch-image?url=' + encodeURIComponent(url), { headers: { 'X-Admin-Token': token } });
    if (!r.ok) throw new Error('대표 사진을 자르지 못했습니다');
    blob = await r.blob();
  }
  const img = await decodeImage(blob);
  const side = Math.min(img.width, img.height), out = Math.min(800, side);
  const c = document.createElement('canvas'); c.width = c.height = out;
  const g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, out, out);
  g.drawImage(img, (img.width - side) / 2, (img.height - side) / 2, side, side, 0, 0, out, out);
  const sq = await canvasBlob(c, 'image/jpeg', 0.85);
  return (await uploadBlob(sq, '대표 사진')).url;
}

/* ---------- 편집기 형식 <-> 사이트 형식 ---------- */
function parseBody(html) { return new DOMParser().parseFromString('<body>' + (html || '') + '</body>', 'text/html').body; }
function unwrap(el) { el.replaceWith(...el.childNodes); }
function figureOf(imgs, cls) {
  const f = document.createElement('figure'); f.className = cls; f.contentEditable = 'false';
  imgs.forEach(i => { const n = document.createElement('img'); n.src = i.getAttribute('src') || ''; n.alt = i.getAttribute('alt') || ''; f.append(n); });
  return f;
}
/** 사이트 형식(저장된 본문) -> 편집기 */
function fromSite(html) {
  const b = parseBody(html);
  $$('.middle-title-box', b).forEach(box => { const h = $('h2', box); if (h) box.replaceWith(h); else unwrap(box); });
  $$('div.detail-thumbnail', b).forEach(d => { const imgs = $$('img', d); if (!imgs.length) d.remove(); else d.replaceWith(figureOf(imgs, 'ed-img')); });
  $$('div.img-pair', b).forEach(d => d.replaceWith(figureOf($$('img', d), 'ed-pair')));
  $$('div.hb-video', b).forEach(d => d.setAttribute('contenteditable', 'false'));
  return b.innerHTML.trim() || '<p><br></p>';
}
/** 편집기 -> 사이트 형식 (detail.css가 꾸미는 모양) */
function toSite(html) {
  const b = parseBody(html);
  $$('[contenteditable]', b).forEach(e => e.removeAttribute('contenteditable'));
  $$('figure', b).forEach(f => {
    const imgs = $$('img', f).filter(i => i.getAttribute('src'));
    if (!imgs.length) return f.remove();
    const d = document.createElement('div');
    d.className = f.classList.contains('ed-pair') ? 'img-pair' : 'detail-thumbnail';
    imgs.forEach(i => { const n = document.createElement('img'); n.src = i.getAttribute('src'); n.alt = i.getAttribute('alt') || ''; d.append(n); });
    f.replaceWith(d);
  });
  $$('h2', b).forEach(h => { if (!h.parentElement.classList.contains('middle-title-box')) { const box = document.createElement('div'); box.className = 'middle-title-box'; h.replaceWith(box); box.append(h); } });
  $$('blockquote', b).forEach(q => q.className = 'hb-quote');
  $$('table', b).forEach(t => t.className = 'detail-table');
  $$('img[data-imp]', b).forEach(i => i.removeAttribute('data-imp'));
  while (b.lastElementChild && b.lastElementChild.tagName === 'P' && !b.lastElementChild.textContent.trim() && !$('img', b.lastElementChild)) b.lastElementChild.remove();
  return b.innerHTML;
}
/** HTML 보기에서 고친 코드를 다시 편집기로: 위험한 것만 지우고 구조는 둠 (서버가 저장할 때 한 번 더 거름) */
function basicClean(html) {
  const b = parseBody(html);
  $$('script,style,link,meta,object,embed,form,input,button,textarea,select', b).forEach(e => e.remove());
  $$('iframe', b).forEach(f => { const id = youtubeId(f.getAttribute('src') || ''); if (!id) f.remove(); });
  $$('*', b).forEach(e => [...e.attributes].forEach(a => { if (/^on/i.test(a.name) || /javascript:/i.test(a.value)) e.removeAttribute(a.name); }));
  return b.innerHTML;
}
function prettyHtml(html) { return html.replace(/(<\/(p|h2|h3|div|li|ul|ol|table|tbody|tr|blockquote)>|<hr>)/g, '$1\n').replace(/\n{2,}/g, '\n').trim(); }
function currentHtml() { return htmlMode ? fromSite(basicClean(htmlView.value)) : editor.innerHTML; }

/* ---------- 커서·블록 넣기 ---------- */
function saveRange() {
  const s = getSelection();
  if (s.rangeCount && editor.contains(s.getRangeAt(0).startContainer)) savedRange = s.getRangeAt(0).cloneRange();
}
document.addEventListener('selectionchange', saveRange);
function restoreRange() {
  if (!savedRange) return;
  const s = getSelection(); s.removeAllRanges(); s.addRange(savedRange);
}
function topBlock(node) {
  if (!node || node === editor) return null;
  while (node && node.parentNode !== editor) node = node.parentNode;
  return node;
}
function isEmptyBlock(el) { return el && el.tagName === 'P' && !el.textContent.trim() && !$('img,iframe', el); }
function placeCaret(el, atStart = true) {
  const r = document.createRange(); r.selectNodeContents(el); r.collapse(atStart);
  const s = getSelection(); s.removeAllRanges(); s.addRange(r); savedRange = r.cloneRange();
}
function newPara() { const p = document.createElement('p'); p.innerHTML = '<br>'; return p; }
function insertBlock(el, caretInside = null) {
  if (htmlMode) toggleHtml();
  let ref = null;
  if (savedRange && editor.contains(savedRange.startContainer)) {
    const c = savedRange.startContainer;
    ref = c === editor ? (editor.childNodes[savedRange.startOffset] || editor.lastChild) : topBlock(c);
  }
  if (ref && ref.nodeType === 3) { const p = document.createElement('p'); ref.replaceWith(p); p.append(ref); ref = p; }
  if (ref && ref.parentNode === editor) { if (isEmptyBlock(ref)) ref.replaceWith(el); else ref.after(el); }
  else editor.append(el);
  let next = el.nextElementSibling;
  if (!next || next.tagName !== 'P') { next = newPara(); el.after(next); }
  editor.focus();
  placeCaret(caretInside || next);
  changed();
  return el;
}

/* ---------- 사진 넣기 ---------- */
async function insertImages(files, pair = false) {
  const list = [...files].filter(f => f && f.type && f.type.startsWith('image/'));
  if (!list.length) return;
  const groups = [];
  if (pair) { for (let i = 0; i < list.length; i += 2) groups.push(list.slice(i, i + 2)); }
  else list.forEach(f => groups.push([f]));
  const jobs = [];
  for (const g of groups) {
    const fig = document.createElement('figure');
    fig.className = (g.length === 2 ? 'ed-pair' : 'ed-img') + ' uploading'; fig.contentEditable = 'false';
    g.forEach(f => { const i = document.createElement('img'); i.src = URL.createObjectURL(f); i.alt = ''; fig.append(i); });
    insertBlock(fig);
    jobs.push(uploadFigure(fig, g));
  }
  await Promise.all(jobs);
  changed();
}
async function uploadFigure(fig, files) {
  const imgs = $$('img', fig);
  try {
    for (let i = 0; i < files.length; i++) {
      const small = await resizeImage(files[i]);
      const data = await uploadBlob(small, (files[i].name || '').replace(/\.[^.]+$/, ''));
      URL.revokeObjectURL(imgs[i].src);
      imgs[i].src = data.url; imgs[i].alt = data.alt === 'houber' ? '' : data.alt;
    }
    fig.classList.remove('uploading');
  } catch (e) {
    fig.remove(); toast('사진을 올리지 못했습니다: ' + e.message, 5000);
  }
}
$('#file-image').addEventListener('change', e => { const pair = e.target.dataset.pair === '1'; insertImages(e.target.files, pair); e.target.value = ''; });

// 사진 선택(클릭) -> Delete/Backspace로 지우기
editor.addEventListener('click', e => {
  $$('figure.sel', editor).forEach(f => f.classList.remove('sel'));
  const f = e.target.closest('figure, .hb-video');
  if (f && editor.contains(f)) f.classList.add('sel');
});
editor.addEventListener('keydown', e => {
  const sel = $('figure.sel, .hb-video.sel', editor);
  if (sel && (e.key === 'Delete' || e.key === 'Backspace')) {
    e.preventDefault(); const next = sel.nextElementSibling; sel.remove(); if (next) placeCaret(next); changed();
  }
});
// 끌어다 놓기
editor.addEventListener('dragover', e => { if ([...e.dataTransfer.types].includes('Files')) { e.preventDefault(); editor.classList.add('drag'); } });
editor.addEventListener('dragleave', () => editor.classList.remove('drag'));
editor.addEventListener('drop', e => {
  editor.classList.remove('drag');
  if (!e.dataTransfer.files.length) return;
  e.preventDefault();
  const r = document.caretRangeFromPoint ? document.caretRangeFromPoint(e.clientX, e.clientY) : null;
  if (r && editor.contains(r.startContainer)) savedRange = r;
  insertImages(e.dataTransfer.files, e.dataTransfer.files.length === 2 && e.shiftKey);
});
// 붙여넣기: 사진이면 올리고, 다른 곳의 서식 있는 글이면 사이트 형식으로 정리
editor.addEventListener('paste', async e => {
  const files = [...e.clipboardData.files].filter(f => f.type.startsWith('image/'));
  if (files.length) { e.preventDefault(); saveRange(); insertImages(files); return; }
  const html = e.clipboardData.getData('text/html');
  if (!html) return;
  e.preventDefault();
  const conv = convertDoc(new DOMParser().parseFromString(html, 'text/html'), { extractTitle: false, keepStyle: false });
  document.execCommand('insertHTML', false, conv.html);
  if (conv.images.length) {
    await rehostImages(conv.images, id => $(`img[data-imp="${id}"]`, editor), src => resolveRemote(src));
  }
  changed();
});

/* ---------- 넣기 버튼 ---------- */
function youtubeId(url) {
  const m = String(url).match(/(?:youtu\.be\/|youtube(?:-nocookie)?\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/|live\/))([A-Za-z0-9_-]{6,20})/);
  return m ? m[1] : null;
}
const actions = {
  image() { const f = $('#file-image'); f.dataset.pair = '0'; f.click(); },
  pair() { const f = $('#file-image'); f.dataset.pair = '1'; toast('사진 두 장을 함께 고르세요'); f.click(); },
  quote() { const q = document.createElement('blockquote'); const p = document.createElement('p'); p.textContent = '인용할 내용을 입력하세요'; q.append(p); insertBlock(q, p); getSelection().selectAllChildren(p); },
  hr() { insertBlock(document.createElement('hr')); },
  link() {
    restoreRange();
    const url = prompt('연결할 주소를 넣어 주세요 (https://...)', 'https://');
    if (!url || !/^(https?:\/\/|mailto:)/i.test(url.trim())) { if (url) toast('https:// 로 시작하는 주소를 넣어 주세요'); return; }
    restoreRange();
    const s = getSelection();
    if (s.rangeCount && !s.isCollapsed) document.execCommand('createLink', false, url.trim());
    else document.execCommand('insertHTML', false, `<a href="${esc(url.trim())}">${esc(url.trim())}</a>&nbsp;`);
    changed();
  },
  table() {
    const t = document.createElement('table'); const tb = document.createElement('tbody'); t.append(tb);
    for (let r = 0; r < 3; r++) { const tr = document.createElement('tr'); for (let c = 0; c < 2; c++) { const td = document.createElement('td'); td.innerHTML = r === 0 && c === 0 ? '항목' : (r === 0 ? '내용' : '<br>'); tr.append(td); } tb.append(tr); }
    const first = $('td', t); insertBlock(t, first); getSelection().selectAllChildren(first);
  },
  youtube() {
    restoreRange();
    const url = prompt('유튜브 영상 주소를 넣어 주세요');
    if (!url) return;
    const id = youtubeId(url);
    if (!id) return toast('유튜브 영상 주소가 아닙니다');
    const d = document.createElement('div'); d.className = 'hb-video'; d.contentEditable = 'false';
    d.innerHTML = `<iframe src="https://www.youtube.com/embed/${id}" title="YouTube" allowfullscreen frameborder="0"></iframe>`;
    insertBlock(d);
  },
  template() { openModal('#template-modal'); },
  import() { resetImport(); openModal('#import-modal'); },
  html() { toggleHtml(); },
};
$$('#ins button').forEach(b => {
  b.addEventListener('mousedown', e => e.preventDefault()); // 누를 때 커서 위치를 잃지 않게
  b.addEventListener('click', () => actions[b.dataset.act]());
});

/* ---------- 글자 서식 ---------- */
$$('#fmt button').forEach(b => {
  b.addEventListener('mousedown', e => e.preventDefault());
  b.addEventListener('click', () => { editor.focus(); restoreRange(); document.execCommand(b.dataset.cmd, false, null); changed(); });
});
$('#fmt-block').addEventListener('change', e => { editor.focus(); restoreRange(); document.execCommand('formatBlock', false, '<' + e.target.value + '>'); changed(); });
$('#fmt-color').addEventListener('change', e => {
  const v = e.target.value; e.target.value = '';
  if (!v) return;
  editor.focus(); restoreRange();
  if (v === 'reset') document.execCommand('removeFormat');
  else { document.execCommand('styleWithCSS', false, true); document.execCommand('foreColor', false, v); document.execCommand('styleWithCSS', false, false); }
  changed();
});
// 커서가 있는 문단 종류를 선택 상자에 표시
document.addEventListener('selectionchange', () => {
  const s = getSelection(); if (!s.rangeCount || !editor.contains(s.anchorNode)) return;
  const b = topBlock(s.anchorNode); if (b && b.tagName) $('#fmt-block').value = ({ H2: 'h2', H3: 'h3' }[b.tagName] || 'p');
});

/* ---------- HTML 보기 ---------- */
function toggleHtml() {
  if (!htmlMode) {
    htmlView.value = prettyHtml(toSite(editor.innerHTML));
    editor.hidden = true; htmlView.hidden = false; htmlMode = true;
  } else {
    editor.innerHTML = fromSite(basicClean(htmlView.value));
    htmlView.hidden = true; editor.hidden = false; htmlMode = false;
  }
  $('#btn-html').classList.toggle('on', htmlMode);
  $('#fmt').classList.toggle('off', htmlMode);
  changed();
}
htmlView.addEventListener('input', changed);

/* ---------- 양식 ---------- */
const TEMPLATES = [
  { name: '빈 글', desc: '아무 틀 없이 자유롭게', html: '' },
  { name: '입주후기 양식', desc: '▪ 입주 정보 (지역·월세·방 구조)<br>▪ 상담부터 입주까지<br>▪ 방 소개 (사진)<br>▪ 주변 환경<br>▪ 한마디',
    html: '<h2>입주 정보</h2><p>지역 / 월세 / 방 구조를 적어 주세요</p><h2>상담부터 입주까지</h2><p>상담을 신청한 계기와 입주까지의 과정을 적어 주세요</p><h2>방 소개</h2><p>사진을 넣어 주세요 (📷 사진)</p><h2>주변 환경</h2><p>역, 편의점, 마트 등 주변 이야기를 적어 주세요</p><h2>한마디</h2><p>입주자분의 한마디를 적어 주세요</p>' },
  { name: '매물 소개 양식', desc: '▪ 한 줄 소개<br>▪ 기본 정보 표 (역·월세·초기비용)<br>▪ 방 사진<br>▪ 추천 포인트<br>▪ 문의 안내',
    html: '<h2>한 줄 소개</h2><p>이 방을 한 줄로 소개해 주세요</p><h2>기본 정보</h2><table><tbody><tr><td>가까운 역</td><td><br></td></tr><tr><td>월세</td><td><br></td></tr><tr><td>초기비용</td><td><br></td></tr><tr><td>방 구조</td><td><br></td></tr></tbody></table><h2>방 사진</h2><p>사진을 넣어 주세요 (📷 사진)</p><h2>추천 포인트</h2><ul><li>추천 포인트를 적어 주세요</li></ul><h2>문의 안내</h2><p>카카오톡으로 편하게 문의해 주세요.</p>' },
];
$('#tpl-list').innerHTML = TEMPLATES.map((t, i) => `<button type="button" data-i="${i}"><b>${t.name}</b>${t.desc}</button>`).join('');
$$('#tpl-list button').forEach(b => b.addEventListener('click', () => {
  const t = TEMPLATES[+b.dataset.i];
  if (htmlMode) toggleHtml();
  if (!t.html) {
    if (!isEditorEmpty() && !confirm('지금 쓴 본문을 지우고 빈 글로 시작할까요?')) return;
    editor.innerHTML = '<p><br></p>';
  } else if (isEditorEmpty()) editor.innerHTML = t.html;
  else editor.insertAdjacentHTML('beforeend', t.html);
  closeModal(b); changed(); toast(`「${t.name}」을 넣었습니다`);
}));

/* ---------- 상태·자동 저장 ---------- */
const draftKey = 'hb-draft-' + (postId || 'new');
function isEditorEmpty() { return !editor.textContent.trim() && !$('img,iframe,table,hr', editor); }
function updatePlaceholder() { editor.classList.toggle('is-empty', !htmlMode && isEditorEmpty() && editor.children.length <= 1); }
let draftTimer = null;
function changed() {
  dirty = true; updatePlaceholder();
  clearTimeout(draftTimer); draftTimer = setTimeout(saveDraft, 1200);
}
function saveDraft() {
  try {
    localStorage.setItem(draftKey, JSON.stringify({ title: titleEl.value, html: currentHtml(), t: Date.now() }));
    const d = new Date(); $('#save-state').textContent = `자동 저장됨 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch (e) { $('#save-state').textContent = ''; }
}
editor.addEventListener('input', changed);
titleEl.addEventListener('input', changed);
window.addEventListener('beforeunload', e => { if (dirty) { saveDraft(); e.preventDefault(); e.returnValue = ''; } });

function showNotice(html) { const n = $('#notice'); n.innerHTML = html; n.hidden = false; }
function init() {
  const initial = fromSite($('#initial-content').value);
  editor.innerHTML = initial;
  updatePlaceholder();
  let draft = null;
  try { draft = JSON.parse(localStorage.getItem(draftKey) || 'null'); } catch (e) { /* 저장소를 못 쓰는 브라우저 */ }
  if (draft && draft.html && (draft.html !== initial || draft.title !== titleEl.value) && (draft.title || draft.html.replace(/<[^>]+>/g, '').trim())) {
    const d = new Date(draft.t);
    showNotice(`저장하지 않은 작성 중인 글이 있습니다 (${d.toLocaleString('ko-KR')}). <button class="btn small" id="draft-load">불러오기</button><button class="btn small" id="draft-drop">버리기</button>`);
    $('#draft-load').onclick = () => { titleEl.value = draft.title || ''; editor.innerHTML = fromSite(toSite(draft.html)); $('#notice').hidden = true; changed(); };
    $('#draft-drop').onclick = () => { try { localStorage.removeItem(draftKey); } catch (e) { /* 무시 */ } $('#notice').hidden = true; };
  }
}

/* ---------- 미리보기·발행 ---------- */
function currentCategory() { return $('#publish-modal').dataset.category || postData.category; }
$('#btn-preview').addEventListener('click', () => {
  const f = $('#preview-form');
  f.title.value = titleEl.value; f.category.value = currentCategory(); f.content.value = toSite(currentHtml());
  f.submit();
});

let selectedThumb = null;
function thumbCandidates() {
  const srcs = [];
  const add = s => { if (s && !srcs.includes(s) && !s.startsWith('blob:')) srcs.push(s); };
  add(pendingThumb); add(postData.thumbnail);
  $$('img', parseBody(currentHtml())).forEach(i => add(i.getAttribute('src')));
  return srcs;
}
function renderThumbs() {
  const list = thumbCandidates();
  if (!selectedThumb || !list.includes(selectedThumb)) selectedThumb = list[0] || null;
  $('#pub-thumbs').innerHTML = list.length ? list.map(s => `<img src="${esc(s)}" class="${s === selectedThumb ? 'on' : ''}" alt="">`).join('') : '<div class="none">본문에 사진이 없습니다. [다른 사진 올리기]로 대표 사진을 올려 주세요</div>';
  $$('#pub-thumbs img').forEach(i => i.addEventListener('click', () => { selectedThumb = i.getAttribute('src'); renderThumbs(); }));
}
$('#btn-publish').addEventListener('click', () => {
  const m = $('#publish-modal');
  if (!titleEl.value.trim()) return toast('제목을 입력해 주세요');
  if ($('figure.uploading', editor)) return toast('사진을 올리는 중입니다. 잠시 후 다시 눌러 주세요');
  $('#pub-category').value = currentCategory();
  if (!m.dataset.opened) {
    $('#pub-description').value = postData.description || '';
    const show = postData.show !== 'false';
    $$('input[name="pub-show"]').forEach(r => (r.checked = (r.value === String(show))));
    m.dataset.opened = '1';
  }
  $('#pub-go').textContent = postId ? '저장' : '발행';
  $('#pub-err').textContent = '';
  renderThumbs();
  openModal('#publish-modal');
});
$('#pub-category').addEventListener('change', e => ($('#publish-modal').dataset.category = e.target.value));
$('#pub-thumb-upload').addEventListener('click', () => $('#file-thumb').click());
$('#file-thumb').addEventListener('change', async e => {
  const f = e.target.files[0]; e.target.value = '';
  if (!f) return;
  try { toast('사진을 올리는 중…'); const data = await uploadBlob(await resizeImage(f), '대표 사진'); pendingThumb = data.url; selectedThumb = data.url; renderThumbs(); }
  catch (err) { $('#pub-err').textContent = err.message; }
});
$('#pub-go').addEventListener('click', async () => {
  const btn = $('#pub-go'); btn.disabled = true; $('#pub-err').textContent = '';
  try {
    if (!selectedThumb) throw new Error('대표 사진을 정해 주세요');
    let thumb = selectedThumb;
    if (thumb !== postData.thumbnail) {
      btn.textContent = '대표 사진 만드는 중…';
      try { thumb = await makeSquareThumb(thumb); } catch (e) { /* 자르지 못하면 원래 사진을 그대로 씀 */ }
    }
    btn.textContent = '저장 중…';
    const body = {
      category: $('#pub-category').value, title: titleEl.value.trim(), content: toSite(currentHtml()),
      thumbnailUrl: thumb, description: $('#pub-description').value.trim(),
      show: $('input[name="pub-show"]:checked').value === 'true',
    };
    const data = postId ? await api('PUT', '/admin/api/posts/' + postId, body) : await api('POST', '/admin/api/posts', body);
    dirty = false;
    try { localStorage.removeItem(draftKey); } catch (e) { /* 무시 */ }
    location.href = '/admin/posts?saved=' + data.id;
  } catch (e) {
    $('#pub-err').textContent = e.message;
    btn.disabled = false; btn.textContent = postId ? '저장' : '발행';
  }
});

/* ---------- 가져오기: HTML/zip -> 사이트 형식 ---------- */
const BLOCK_TAGS = new Set(['p', 'div', 'section', 'article', 'main', 'header', 'footer', 'center', 'address', 'pre', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'table', 'blockquote', 'hr', 'img', 'figure', 'figcaption', 'dl', 'dt', 'dd', 'nav', 'aside', 'body']);
function hasBlockInside(el) { return !!el.querySelector([...BLOCK_TAGS].join(',') + ',[data-yt]'); }
function alignOf(el) { const s = (el.getAttribute('style') || '') + ' ' + (el.getAttribute('align') || ''); return /text-align\s*:\s*center|center/i.test(s) ? 'center' : null; }

/** 글자 단위 내용만 남김 (굵게·기울임·밑줄·링크, 원하면 글자색) */
function convertInline(node, opts) {
  const frag = document.createDocumentFragment();
  for (const ch of node.childNodes) {
    if (ch.nodeType === 3) { frag.append(document.createTextNode(ch.nodeValue.replace(/[\r\n\t]+/g, ' '))); continue; }
    if (ch.nodeType !== 1) continue;
    const tag = ch.localName.toLowerCase();
    if (tag === 'br') { frag.append(document.createElement('br')); continue; }
    let wrap = null;
    if (tag === 'b' || tag === 'strong') wrap = document.createElement('b');
    else if (tag === 'i' || tag === 'em') wrap = document.createElement('i');
    else if (tag === 'u') wrap = document.createElement('u');
    else if (tag === 'a') {
      const href = (ch.getAttribute('href') || '').trim();
      if (/^(https?:|mailto:)/i.test(href)) { wrap = document.createElement('a'); wrap.href = href; }
    } else if (opts.keepStyle) {
      const color = (ch.getAttribute('color') || ((ch.getAttribute('style') || '').match(/(?:^|;)\s*color\s*:\s*([^;]+)/i) || [])[1] || '').trim();
      if (color && !/url\(|expression/i.test(color)) { wrap = document.createElement('span'); wrap.style.color = color; }
    }
    if (!wrap) { const b = (ch.getAttribute('style') || '').match(/font-weight\s*:\s*(bold|[6-9]00)/i); if (b) wrap = document.createElement('b'); }
    const inner = convertInline(ch, opts);
    if (wrap) { wrap.append(inner); frag.append(wrap); } else frag.append(inner);
  }
  return frag;
}

function convertDoc(doc, opts) {
  const stats = { styles: 0, scripts: 0, floats: 0 };
  const body = doc.body || doc.documentElement;
  stats.scripts = doc.querySelectorAll('script').length; // <head>에 있는 것까지 셈 (본문만 가져오므로 어차피 들어가지 않음)
  $$('script,noscript', body).forEach(e => e.remove());
  $$('style,link,meta,title,object,embed,form,input,button,select,textarea,svg,canvas,audio,video,map,head', body).forEach(e => e.remove());
  const tw = doc.createTreeWalker(body, NodeFilter.SHOW_COMMENT); const comments = []; while (tw.nextNode()) comments.push(tw.currentNode); comments.forEach(c => c.remove());
  $$('iframe', body).forEach(f => { const id = youtubeId(f.getAttribute('src') || ''); if (id) { const d = doc.createElement('div'); d.setAttribute('data-yt', id); f.replaceWith(d); } else f.remove(); });
  $$('[style]', body).forEach(e => { stats.styles++; if (/float\s*:\s*(left|right)/i.test(e.getAttribute('style'))) stats.floats++; });
  $$('[align]', body).forEach(e => { if (/left|right/i.test(e.getAttribute('align')) && e.localName === 'img') stats.floats++; });
  stats.styles += $$('font', body).length;
  let title = '';
  if (opts.extractTitle) {
    const h = $('h1', body) || $('h2', body);
    if (h && h.textContent.trim()) { title = h.textContent.replace(/\s+/g, ' ').trim(); h.remove(); }
    else if (doc.title && doc.title.trim()) title = doc.title.trim();
  }
  const out = document.createElement('div');
  const ctx = { out, para: null, images: [], opts };
  walk(body, ctx);
  flushPara(ctx);
  $$('p', out).forEach(p => { if (!p.textContent.replace(/ /g, ' ').trim()) p.remove(); });
  $$('h2,h3,li,td', out).forEach(e => { if (e.localName !== 'td' && !e.textContent.trim()) e.remove(); });
  return {
    html: out.innerHTML, title, images: ctx.images,
    stats: Object.assign(stats, { headings: $$('h2,h3', out).length, paragraphs: $$('p', out).length, tables: $$('table', out).length, lists: $$('ul,ol', out).length }),
  };
}
function newParaCtx(ctx, align) { ctx.para = document.createElement('p'); if (align) ctx.para.style.textAlign = align; }
function flushPara(ctx) {
  if (ctx.para && ctx.para.textContent.trim()) {
    while (ctx.para.lastChild && ctx.para.lastChild.nodeName === 'BR') ctx.para.lastChild.remove();
    ctx.out.append(ctx.para);
  }
  ctx.para = null;
}
function appendInline(ctx, node) { if (!ctx.para) newParaCtx(ctx); ctx.para.append(node); }
function addImage(ctx, src, alt) {
  flushPara(ctx);
  const id = ctx.images.length;
  ctx.images.push({ id, src: (src || '').trim() });
  const f = document.createElement('figure'); f.className = 'ed-img'; f.contentEditable = 'false';
  const img = document.createElement('img'); img.setAttribute('data-imp', id); img.alt = alt || ''; f.append(img);
  ctx.out.append(f);
}
function walk(node, ctx) {
  for (const ch of [...node.childNodes]) {
    if (ch.nodeType === 3) {
      const t = ch.nodeValue.replace(/[\r\n\t]+/g, ' ');
      if (t.trim() || (ctx.para && t)) appendInline(ctx, document.createTextNode(t));
      continue;
    }
    if (ch.nodeType !== 1) continue;
    const tag = ch.localName.toLowerCase();
    if (/^h[1-6]$/.test(tag)) {
      flushPara(ctx);
      $$('img', ch).forEach(i => addImage(ctx, i.getAttribute('src'), i.getAttribute('alt')));
      const h = document.createElement(tag === 'h1' || tag === 'h2' ? 'h2' : 'h3'); h.append(convertInline(ch, ctx.opts));
      if (h.textContent.trim()) ctx.out.append(h);
    } else if (tag === 'p' || tag === 'center') {
      flushPara(ctx); newParaCtx(ctx, tag === 'center' ? 'center' : alignOf(ch));
      if (hasBlockInside(ch)) walk(ch, ctx); else ctx.para.append(convertInline(ch, ctx.opts));
      flushPara(ctx);
    } else if (tag === 'br') {
      if (ctx.para) ctx.para.append(document.createElement('br'));
    } else if (tag === 'ul' || tag === 'ol') {
      flushPara(ctx);
      const list = document.createElement(tag);
      $$(':scope > li', ch).forEach(li => { $$('img', li).forEach(i => addImage(ctx, i.getAttribute('src'), i.getAttribute('alt'))); const n = document.createElement('li'); n.append(convertInline(li, ctx.opts)); list.append(n); });
      if (list.children.length) ctx.out.append(list);
    } else if (tag === 'table') {
      flushPara(ctx);
      const t = document.createElement('table'); const tb = document.createElement('tbody'); t.append(tb);
      $$('tr', ch).filter(tr => tr.closest('table') === ch).forEach(tr => {
        const r = document.createElement('tr');
        $$(':scope > td, :scope > th', tr).forEach(c => { const n = document.createElement('td'); n.append(convertInline(c, ctx.opts)); if (c.localName === 'th') { const b = document.createElement('b'); b.append(...n.childNodes); n.append(b); } if (c.getAttribute('colspan')) n.setAttribute('colspan', c.getAttribute('colspan')); r.append(n); });
        if (r.children.length) tb.append(r);
      });
      if (tb.children.length) ctx.out.append(t);
      $$('img', ch).forEach(i => addImage(ctx, i.getAttribute('src'), i.getAttribute('alt'))); // 표 안의 사진은 표 다음에
    } else if (tag === 'blockquote') {
      flushPara(ctx);
      const q = document.createElement('blockquote'); const p = document.createElement('p'); p.append(convertInline(ch, ctx.opts)); q.append(p);
      if (p.textContent.trim()) ctx.out.append(q);
    } else if (tag === 'hr') {
      flushPara(ctx); ctx.out.append(document.createElement('hr'));
    } else if (tag === 'img') {
      addImage(ctx, ch.getAttribute('src'), ch.getAttribute('alt'));
    } else if (ch.hasAttribute('data-yt')) {
      flushPara(ctx);
      const d = document.createElement('div'); d.className = 'hb-video'; d.contentEditable = 'false';
      d.innerHTML = `<iframe src="https://www.youtube.com/embed/${ch.getAttribute('data-yt')}" title="YouTube" allowfullscreen frameborder="0"></iframe>`;
      ctx.out.append(d);
    } else if (BLOCK_TAGS.has(tag)) {
      flushPara(ctx);
      const align = alignOf(ch);
      if (align && !hasBlockInside(ch)) { newParaCtx(ctx, align); ctx.para.append(convertInline(ch, ctx.opts)); flushPara(ctx); }
      else walk(ch, ctx);
      flushPara(ctx);
    } else if (hasBlockInside(ch)) {
      walk(ch, ctx); // 글자 태그 안에 사진·문단이 들어 있는 경우
    } else {
      const holder = document.createElement('span'); holder.append(ch.cloneNode(true));
      appendInline(ctx, convertInline(holder, ctx.opts));
    }
  }
}

/** 사진 원본 받기: data: 주소 / 인터넷 주소(서버가 대신 받음) */
async function resolveRemote(src) {
  if (!src) return null;
  if (src.startsWith('data:')) return (await fetch(src)).blob();
  let url = src.startsWith('//') ? 'https:' + src : src;
  if (!/^https?:\/\//i.test(url)) return null;
  const r = await fetch('/admin/api/fetch-image?url=' + encodeURIComponent(url), { headers: { 'X-Admin-Token': token } });
  if (!r.ok) return null;
  return r.blob();
}
/** 사진을 하우버 서버로 옮기고 img 주소를 바꿈. 못 찾은 사진은 지우고 목록으로 돌려줌 */
async function rehostImages(images, findImg, resolver, onProgress) {
  const res = { moved: 0, missing: [], before: 0, after: 0 };
  for (let i = 0; i < images.length; i++) {
    const im = images[i]; const img = findImg(im.id);
    if (!img) continue;
    const fig = img.closest('figure'); if (fig) fig.classList.add('uploading');
    if (onProgress) onProgress(i + 1, images.length);
    let blob = null;
    try { blob = await resolver(im.src); } catch (e) { blob = null; }
    if (blob && !/^image\//.test(blob.type)) blob = new Blob([blob], { type: guessType(im.src) });
    try {
      if (!blob) throw new Error('missing');
      res.before += blob.size;
      const small = await resizeImage(blob);
      const data = await uploadBlob(small, img.alt);
      res.after += small.size; res.moved++;
      img.src = data.url; img.removeAttribute('data-imp');
      if (fig) fig.classList.remove('uploading');
    } catch (e) {
      res.missing.push(im.src.startsWith('data:') ? '(파일 안의 사진)' : decodeURIComponentSafe(im.src));
      if (fig) fig.remove(); else img.remove();
    }
  }
  return res;
}
function guessType(name) { const n = name.toLowerCase(); return /\.png($|\?)/.test(n) ? 'image/png' : /\.gif($|\?)/.test(n) ? 'image/gif' : /\.webp($|\?)/.test(n) ? 'image/webp' : 'image/jpeg'; }
function decodeURIComponentSafe(s) { try { return decodeURIComponent(s); } catch (e) { return s; } }

/** HTML 파일 글자 인코딩 판별 (워드·한글로 저장한 파일은 EUC-KR인 경우가 많음) */
function decodeHtml(buf) {
  const head = new TextDecoder('latin1').decode(buf.slice(0, 4096));
  let cs = (head.match(/charset\s*=\s*["']?([A-Za-z0-9_.:-]+)/i) || [])[1] || 'utf-8';
  cs = cs.toLowerCase();
  if (/ks_c_5601|euc-?kr|cp949|windows-949|x-windows-949/.test(cs)) cs = 'euc-kr';
  try { return new TextDecoder(cs).decode(buf); } catch (e) { return new TextDecoder('utf-8').decode(buf); }
}

let JSZipLoaded = null;
function loadJSZip() {
  if (window.JSZip) return Promise.resolve();
  if (!JSZipLoaded) JSZipLoaded = new Promise((ok, fail) => { const s = document.createElement('script'); s.src = 'https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js'; s.onload = ok; s.onerror = () => fail(new Error('zip 도구를 불러오지 못했습니다')); document.head.append(s); });
  return JSZipLoaded;
}
function joinPath(dir, rel) {
  const parts = (dir ? dir.split('/') : []);
  rel.split(/[\\/]/).forEach(p => { if (p === '..') parts.pop(); else if (p && p !== '.') parts.push(p); });
  return parts.join('/');
}

/* 가져오기 창 */
let impTab = 'html', impFile = null, impResult = null;
function resetImport() {
  impFile = null; impResult = null;
  $('#imp-step1').hidden = false; $('#imp-step2').hidden = true;
  $('#imp-file').hidden = true; $('#imp-err').textContent = ''; $('#imp-blog-url').value = '';
  setImpTab('html');
}
function setImpTab(tab) {
  impTab = tab; impFile = null; $('#imp-file').hidden = true; $('#imp-err').textContent = '';
  $$('#imp-tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === tab));
  $$('[data-pane]').forEach(p => (p.hidden = p.dataset.pane !== tab));
  $('#imp-opts').hidden = tab === 'blog';
  $('#imp-go').disabled = tab !== 'blog';
}
$$('#imp-tabs button').forEach(b => b.addEventListener('click', () => setImpTab(b.dataset.tab)));
function pickImportFile(f) {
  if (!f) return;
  const ok = impTab === 'zip' ? /\.zip$/i.test(f.name) : /\.html?$/i.test(f.name);
  if (!ok) { $('#imp-err').textContent = impTab === 'zip' ? '.zip 파일을 골라 주세요' : '.html 파일을 골라 주세요 (사진 폴더가 따로 있으면 zip 탭)'; return; }
  impFile = f;
  $('#imp-file').hidden = false;
  $('#imp-file').innerHTML = `<span>${impTab === 'zip' ? '🗜' : '📄'} <b>${esc(f.name)}</b> · ${f.size < 1048576 ? Math.ceil(f.size / 1024) + 'KB' : mb(f.size)}</span><span style="color:#999;cursor:pointer" id="imp-clear">✕</span>`;
  $('#imp-clear').onclick = () => { impFile = null; $('#imp-file').hidden = true; $('#imp-go').disabled = true; };
  $('#imp-err').textContent = ''; $('#imp-go').disabled = false;
}
$$('.dz').forEach(dz => {
  dz.addEventListener('click', () => { const f = $('#file-import'); f.accept = impTab === 'zip' ? '.zip' : '.html,.htm'; f.click(); });
  dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('drag'); });
  dz.addEventListener('dragleave', () => dz.classList.remove('drag'));
  dz.addEventListener('drop', e => { e.preventDefault(); dz.classList.remove('drag'); pickImportFile(e.dataTransfer.files[0]); });
});
$('#file-import').addEventListener('change', e => { pickImportFile(e.target.files[0]); e.target.value = ''; });
$('#imp-blog-url').addEventListener('input', () => ($('#imp-go').disabled = !$('#imp-blog-url').value.trim()));

$('#imp-go').addEventListener('click', async () => {
  const btn = $('#imp-go'); btn.disabled = true; $('#imp-err').textContent = '';
  const label = btn.textContent;
  try {
    impResult = impTab === 'blog' ? await importBlog(btn) : await importFile(btn);
    showImportResult(impResult);
  } catch (e) {
    $('#imp-err').textContent = e.message;
  } finally { btn.disabled = false; btn.textContent = label; }
});

async function importFile(btn) {
  if (!impFile) throw new Error('파일을 골라 주세요');
  const opts = { extractTitle: $('#opt-title').checked, keepStyle: !$('#opt-style').checked };
  let htmlText, resolver, sourceName = impFile.name;
  if (impTab === 'zip') {
    btn.textContent = 'zip 여는 중…';
    await loadJSZip();
    const zip = await window.JSZip.loadAsync(impFile, {
      decodeFileName: bytes => { try { return new TextDecoder('utf-8', { fatal: true }).decode(bytes); } catch (e) { return new TextDecoder('euc-kr').decode(bytes); } },
    });
    const files = Object.values(zip.files).filter(f => !f.dir && !/__MACOSX\//.test(f.name));
    const htmls = files.filter(f => /\.html?$/i.test(f.name));
    if (!htmls.length) throw new Error('zip 안에 .html 파일이 없습니다');
    let main = htmls[0], size = -1;
    for (const h of htmls) { const s = (await h.async('uint8array')).length; if (s > size) { size = s; main = h; } }
    sourceName = main.name;
    htmlText = decodeHtml((await main.async('uint8array')).buffer);
    const dir = main.name.includes('/') ? main.name.slice(0, main.name.lastIndexOf('/')) : '';
    const byLower = new Map(files.map(f => [f.name.toLowerCase(), f]));
    resolver = async src => {
      if (/^(data:|https?:|\/\/)/i.test(src)) return resolveRemote(src);
      const path = joinPath(dir, decodeURIComponentSafe(src.split(/[?#]/)[0]));
      const f = zip.file(path) || byLower.get(path.toLowerCase());
      if (!f) return null;
      return new Blob([await f.async('uint8array')], { type: guessType(path) });
    };
  } else {
    htmlText = decodeHtml(await impFile.arrayBuffer());
    resolver = src => resolveRemote(src);
  }
  btn.textContent = '변환 중…';
  const doc = new DOMParser().parseFromString(htmlText, 'text/html');
  const conv = convertDoc(doc, opts);
  const holder = document.createElement('div'); holder.innerHTML = conv.html;
  const moved = await rehostImages(conv.images, id => $(`img[data-imp="${id}"]`, holder), resolver,
    (i, n) => (btn.textContent = `사진 옮기는 중… (${i}/${n})`));
  return { kind: 'file', sourceName, original: htmlText, title: conv.title, html: holder.innerHTML, stats: conv.stats, images: conv.images.length, moved };
}

async function importBlog(btn) {
  btn.textContent = '블로그 글 가져오는 중… (사진이 많으면 1분쯤 걸려요)';
  const url = $('#imp-blog-url').value.trim();
  const data = await api('POST', '/admin/api/import/blog', { url });
  return { kind: 'blog', sourceName: url, title: data.title, html: fromSite(data.content), description: data.description, thumbnailUrl: data.thumbnailUrl, images: data.images };
}

function siteDoc(html) {
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><link rel="stylesheet" href="/css/main.css"><link rel="stylesheet" href="/css/detail.css"></head><body style="padding:10px 14px"><section class="detail-content" style="margin:0"><div id="detail-content">${html}</div></section></body></html>`;
}
function showImportResult(r) {
  $('#imp-step1').hidden = true; $('#imp-step2').hidden = false;
  if (r.kind === 'blog') {
    $('#imp-left-title').textContent = '원래 글 (네이버 블로그)';
    $('#imp-orig').srcdoc = `<p style="font:14px sans-serif;padding:14px">원래 글: <a href="${esc(r.sourceName)}" target="_blank">${esc(r.sourceName)}</a></p>`;
  } else {
    $('#imp-left-title').textContent = '원래 파일 (' + r.sourceName + ')';
    // 원래 파일 미리보기: 스크립트·iframe·외부 연결은 막고 사진만 보이게
    $('#imp-orig').srcdoc = '<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; img-src data: https: http:; style-src \'unsafe-inline\'">' + r.original;
  }
  $('#imp-conv').srcdoc = siteDoc((r.title ? `<div class="detail-header"><h1>${esc(r.title)}</h1></div>` : '') + toSite(r.html));
  const lines = [];
  if (r.title) lines.push(`<span class="ok">✔ 제목</span> 「${esc(r.title)}」 → 글 제목으로 사용`);
  if (r.kind === 'blog') {
    lines.push(`<span class="ok">✔ 블로그 글을 사이트 형식으로 변환</span>${r.images ? ` (사진 ${r.images}장 하우버 서버로 옮김)` : ''}`);
  } else {
    const m = r.moved;
    if (r.images) lines.push(`<span class="ok">✔ 사진 ${m.moved}장</span> 하우버 서버로 옮김 (화면용 크기로 맞춤${m.after < m.before * 0.9 ? `, 합계 ${mb(m.before)} → ${mb(m.after)}` : ''})`);
    const s = r.stats;
    lines.push(`<span class="ok">✔ 소제목 ${s.headings}개, 문단 ${s.paragraphs}개${s.tables ? `, 표 ${s.tables}개` : ''}${s.lists ? `, 목록 ${s.lists}개` : ''}</span> 사이트 스타일로 변환`);
    const removed = [];
    if (s.styles) removed.push(`글꼴·색·크기 설정 ${s.styles}곳`);
    if (s.floats) removed.push(`사진 옆 배치 ${s.floats}곳`);
    if (s.scripts) removed.push(`스크립트 ${s.scripts}개`);
    if (removed.length) lines.push(`<span class="warn">△ 지운 것</span> ${removed.join(', ')}`);
    if (m.missing.length) lines.push(`<span class="warn">△ 확인 필요</span> 사진 ${m.missing.length}장을 찾을 수 없어 뺐습니다: <code>${m.missing.slice(0, 5).map(esc).join(', ')}${m.missing.length > 5 ? ' 외' : ''}</code>${impTab === 'html' ? ' (사진 폴더까지 zip으로 올리면 함께 가져옵니다)' : ''}`);
  }
  $('#imp-report').innerHTML = '<b>변환 결과</b><br>' + lines.join('<br>') + '<br><span class="sub">아직 사이트에는 올라가지 않았습니다. 편집기로 불러와 확인한 뒤 [발행]하세요.</span>';
}
$('#imp-back').addEventListener('click', resetImport);
$('#imp-apply').addEventListener('click', () => {
  const r = impResult; if (!r) return;
  if (htmlMode) toggleHtml();
  if (!isEditorEmpty() && !confirm('편집기에 쓰던 내용을 가져온 글로 바꿀까요?\n[취소]를 누르면 쓰던 내용 뒤에 붙입니다.')) editor.insertAdjacentHTML('beforeend', r.html);
  else editor.innerHTML = r.html || '<p><br></p>';
  if (r.title && (!titleEl.value.trim() || confirm(`제목을 「${r.title}」(으)로 바꿀까요?`))) titleEl.value = r.title;
  if (r.description && !postData.description) postData.description = r.description;
  if (r.thumbnailUrl) { pendingThumb = r.thumbnailUrl; selectedThumb = r.thumbnailUrl; }
  closeModal($('#imp-apply'));
  showNotice(`⇪ ${esc(r.sourceName)}에서 가져온 글입니다. 내용을 확인하고 고친 뒤 [발행]하세요. 아직 사이트에는 보이지 않습니다.`);
  changed();
});

init();
