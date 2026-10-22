'use strict';
if (!location.pathname.endsWith('/') && !location.pathname.endsWith('.html')) location.replace(location.pathname + '/' + location.search);
const $ = id => document.getElementById(id);
const base = new URL('./', location.href);
let sessionId = null, busy = false;
const initial = $('messages').firstElementChild.cloneNode(true);
function append(role, text, route) {
  const article = document.createElement('article'); article.className = 'message ' + role;
  const avatar = document.createElement('span'); avatar.className = 'avatar'; avatar.textContent = role === 'user' ? 'Y' : route === 'TECH' ? 'A' : route === 'BILLING' ? 'B' : 'R';
  const content = document.createElement('div'), label = document.createElement('p'), body = document.createElement('p');
  label.className = 'message-label'; label.textContent = role === 'user' ? 'YOU' : (route || 'RELAY') + ' / SAMPLE'; body.textContent = text;
  content.append(label, body); article.append(avatar, content); $('messages').append(article); $('messages').scrollTop = $('messages').scrollHeight;
}
function inspect(data) {
  $('route').textContent = data.route; $('reason').textContent = data.reason;
   $('tools').replaceChildren(); $('sources').replaceChildren();
  if (!data.tools.length) $('tools').textContent = 'No tools needed';
  data.tools.forEach(tool => { const el = document.createElement('span'); el.className = 'tool'; el.textContent = tool; $('tools').append(el); });
  if (!data.sources.length) $('sources').textContent = 'No documentation used in this reply.';
  data.sources.forEach(source => { const details = document.createElement('details'), title = document.createElement('summary'), body = document.createElement('p'); details.className = 'source'; title.textContent = source.id; body.textContent = source.text; details.append(title, body); $('sources').append(details); });
  $('turns').textContent = data.turn + ' / 30 turns'; $('refund-help').hidden = data.route !== 'BILLING' || !data.reason.includes('waiting');
}
async function send(text) {
  if (busy || !text.trim()) return;
  busy = true; $('send').disabled = true; $('reset').disabled = true; document.querySelectorAll('[data-prompt],[data-followup]').forEach(b => b.disabled = true);
  append('user', text); $('message').value = ''; $('status').textContent = 'Routing your message…';
  try {
    const response = await fetch(new URL('api/chat', base), {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({sessionId,message:text}),signal:AbortSignal.timeout(15000)});
    if (!response.ok) {
      if (response.status === 410) { sessionId = null; throw new Error('Your session expired. Send the message again to start a new one.'); }
      throw new Error(response.status === 429 ? 'Demo limit reached. Wait a minute, or start a new conversation after 30 turns.' : response.status === 400 ? 'Please enter 1–2000 characters.' : 'The service is temporarily unavailable. Please try again.');
    }
    const data = await response.json(); sessionId = data.sessionId; append('assistant', data.answer, data.route); inspect(data); $('status').textContent = '';
  } catch (e) { $('status').textContent = e.name === 'TimeoutError' ? 'The request timed out. Please try again.' : e.message; $('message').value = text; }
  finally { busy = false; $('send').disabled = false; $('reset').disabled = false; document.querySelectorAll('[data-prompt],[data-followup]').forEach(b => b.disabled = false); }
}
$('chat-form').addEventListener('submit', e => { e.preventDefault(); send($('message').value); });
$('message').addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send($('message').value); } });
document.querySelectorAll('[data-prompt]').forEach(b => b.addEventListener('click', () => send(b.dataset.prompt)));
document.querySelectorAll('[data-followup]').forEach(b => b.addEventListener('click', () => send(b.dataset.followup)));
$('reset').addEventListener('click', async () => {
  if (busy) return;
  if (sessionId) {
    try { const r = await fetch(new URL('api/session', base), {method:'DELETE',headers:{'Content-Type':'application/json'},body:JSON.stringify({sessionId}),signal:AbortSignal.timeout(10000)}); if (!r.ok) throw new Error(); }
    catch { $('status').textContent = 'Could not clear the server session. Please retry.'; return; }
  }
  sessionId = null; $('messages').replaceChildren(initial.cloneNode(true)); $('status').textContent = ''; $('message').value = '';
  inspect({route:'Waiting for a message',reason:'The route and matching evidence appear here after each response.',tools:[],sources:[],turn:0}); $('message').focus();
});
fetch(new URL('api/health', base), {signal:AbortSignal.timeout(10000)}).then(r => { if(!r.ok) throw new Error(); return r.json(); }).then(() => $('health').textContent = 'Service available · sample mode').catch(() => $('health').textContent = 'Service unavailable · try again later');
