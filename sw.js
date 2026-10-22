/* hoanghaoZ - BỘ NHỚ TẠM NGOẠI TUYẾN (service worker)
   Nhờ tệp này mà mất wifi lớp vẫn mở được app và chơi tiếp.
   Mỗi lần phát hành bản mới: đổi VERSION bên dưới thành 'hz-v2', 'hz-v3'... */

const VERSION = 'hz-v2';
const SHELL = ['new.html', 'manifest.json', 'icon-512.png', 'splash-character.png'];

/* Chỉ những nơi này được lưu lại. Database Firebase và máy AI không bị chạm tới. */
const CACHE_HOSTS = [
  location.hostname,
  'fonts.googleapis.com',
  'fonts.gstatic.com',
  'www.gstatic.com',
  'cdn.jsdelivr.net'
];

function cacheable(url) {
  if (url.protocol !== 'http:' && url.protocol !== 'https:') return false;
  /* Kho Kien thuc (kien-thuc/*.html) phai luon lay ban moi: co sua noi dung
     la hoc sinh thay ngay, khong bi ban luu tam cu om. */
  if (url.pathname.indexOf('/kien-thuc/') !== -1) return false;
  const h = url.hostname;
  if (h.endsWith('firebaseio.com') || h.endsWith('firebasedatabase.app')) return false;
  if (h === 'generativelanguage.googleapis.com') return false;
  return CACHE_HOSTS.indexOf(h) !== -1;
}

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(VERSION)
      .then((c) => Promise.all(SHELL.map((f) => c.add('./' + f).catch(() => null))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;

  let url;
  try { url = new URL(req.url); } catch (_) { return; }
  if (!cacheable(url)) return;

  /* Mở trang: thử xin mạng trước. Mất mạng mới lấy bản đã lưu ra cho chơi. */
  if (req.mode === 'navigate' || req.destination === 'document') {
    e.respondWith(
      fetch(req, { cache: 'no-store' })
        .then((res) => {
          if (res && res.ok) {
            caches.open(VERSION).then((c) => {
              c.put('./new.html', res.clone());
              c.put(req.url, res.clone());
            });
          }
          return res;
        })
        .catch(async () => {
          const c = await caches.open(VERSION);
          return (await c.match(req.url)) || (await c.match('./new.html'));
        })
    );
    return;
  }

  /* Font, thư viện, ảnh: dùng bản cũ ngay lập tức, tải lại ngầm phía sau. */
  e.respondWith(
    caches.open(VERSION).then(async (c) => {
      const cached = await c.match(req);
      const net = fetch(req).then((res) => {
        if (res && (res.ok || res.type === 'opaque')) c.put(req, res.clone());
        return res;
      }).catch(() => cached);
      return cached || net;
    })
  );
});
