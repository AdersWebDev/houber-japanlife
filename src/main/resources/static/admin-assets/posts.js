// 글 관리 목록: 노출/숨김, 삭제
const token = document.querySelector('meta[name="admin-token"]').content;
function toast(msg) { const t = document.getElementById('toast'); t.textContent = msg; t.style.display = 'block'; setTimeout(() => t.style.display = 'none', 2500); }
async function api(method, url, body) {
  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json', 'X-Admin-Token': token }, body: body ? JSON.stringify(body) : undefined });
  if (res.status === 401) { location.href = '/admin/login'; throw new Error('login'); }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || ('오류 ' + res.status));
  return data;
}
async function setShow(btn, show) {
  const id = btn.closest('tr').dataset.id;
  btn.disabled = true;
  try { await api('PATCH', `/admin/api/posts/${id}/show`, { show }); location.reload(); }
  catch (e) { toast(e.message); btn.disabled = false; }
}
async function removePost(btn) {
  const tr = btn.closest('tr');
  const title = tr.querySelector('td.title a').textContent;
  if (!confirm(`「${title}」을(를) 삭제할까요?\n삭제하면 되돌릴 수 없습니다. 잠시 내리려면 [숨기기]를 쓰세요.`)) return;
  btn.disabled = true;
  try { await api('DELETE', `/admin/api/posts/${tr.dataset.id}`); tr.remove(); toast('삭제했습니다'); }
  catch (e) { toast(e.message); btn.disabled = false; }
}
const saved = new URLSearchParams(location.search).get('saved');
if (saved) toast('저장했습니다');
