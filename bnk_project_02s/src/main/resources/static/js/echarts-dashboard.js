// static/js/echarts-dashboard.js

/* ================== 0) ECharts 파란 테마 등록 + 유틸 ================== */
(function registerBlueTheme(){
  if (typeof echarts === 'undefined') return;
  const BLUE_THEME = {
    color: ['#3b82f6','#60a5fa','#93c5fd','#1d4ed8','#38bdf8','#818cf8'],
    backgroundColor: 'transparent',
    textStyle: { color: '#0f172a' },
    legend: { textStyle: { color: '#334155' } },
    tooltip: {
      backgroundColor: '#fff',
      borderColor: '#dbeafe',
      borderWidth: 1,
      textStyle: { color: '#0f172a' },
      shadowBlur: 8,
      shadowColor: 'rgba(37,99,235,.12)'
    },
    axisPointer:{ lineStyle:{ color:'#3b82f6' } },
    grid: { left: 30, right: 20, top: 32, bottom: 28, containLabel: true },
    categoryAxis: {
      axisLine: { lineStyle: { color:'#9db2d7' } },
      axisTick: { show:false },
      axisLabel:{ color:'#476082' },
      splitLine:{ show:false }
    },
    valueAxis: {
      axisLine: { show:false },
      axisTick: { show:false },
      axisLabel:{ color:'#476082' },
      splitLine:{ show:true, lineStyle:{ color:'#eaf1ff' } }
    },
    line: { lineStyle:{ width:3 }, symbol:'circle', symbolSize:6, smooth:true },
    bar:  { itemStyle:{ borderRadius:[8,8,0,0] } },
    pie:  { itemStyle:{ borderColor:'#fff', borderWidth:2 }, label:{ color:'#0f172a' } }
  };
  echarts.registerTheme('blue', BLUE_THEME);
})();

// 그래픽 헬퍼(그라데이션, 막대 스타일)
const G = echarts.graphic;
const blueArea = (cTop='#93c5fd') =>
  ({ color: new G.LinearGradient(0,0,0,1,[{offset:0,color:cTop},{offset:1,color:'rgba(147,197,253,0.06)'}]) });
const blueBar = () =>
  ({ color: new G.LinearGradient(0,0,0,1,[{offset:0,color:'#60a5fa'},{offset:1,color:'#3b82f6'}]),
     shadowBlur:8, shadowColor:'rgba(37,99,235,.18)', shadowOffsetY:4, borderRadius:[8,8,0,0] });


/* ================== 1) 차트 렌더 ================== */
document.addEventListener('DOMContentLoaded', () => {

  /* ───────────── 날짜 유틸 ───────────── */
  const today = new Date();
  const lastNDays = (n) =>
    Array.from({ length: n }, (_, i) => {
      const d = new Date(today);
      d.setDate(today.getDate() - (n - 1 - i));
      return `${d.getMonth() + 1}/${d.getDate()}`;
    });
  const lastNQuarters = (n) =>
    Array.from({ length: n }, (_, i) => {
      const d = new Date(today);
      d.setMonth(today.getMonth() - 3 * (n - 1 - i));
      const q = Math.floor(d.getMonth() / 3) + 1;
      return `${d.getFullYear()} Q${q}`;
    });
  const lastNMonths = (n) =>
    Array.from({ length: n }, (_, i) => {
      const d = new Date(today);
      d.setMonth(today.getMonth() - (n - 1 - i));
      return `${d.getMonth() + 1}월`;
    });

  const dailyLabels     = lastNDays(7);
  const monthlyLabels   = lastNMonths(6);
  const quarterlyLabels = lastNQuarters(6);

  /* ── 가입자수 라인 (일/월/분기) ── */
  const dailyChart = echarts.init(document.getElementById('chart-daily'), 'blue');
  dailyChart.setOption({
    xAxis:{ type:'category', data: dailyLabels },
    yAxis:{ type:'value' },
    tooltip:{ trigger:'axis', formatter: p => `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명` },
    series:[{ type:'line', data:[5,9,6,7,10], areaStyle: blueArea('#93c5fd'),
              itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)'} }]
  });

  const monthlyChart = echarts.init(document.getElementById('chart-monthly'), 'blue');
  monthlyChart.setOption({
    xAxis:{ type:'category', data: monthlyLabels },
    yAxis:{ type:'value' },
    tooltip:{ trigger:'axis', formatter: p => `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명` },
    series:[{ type:'line', data:[30,50,60,80,120], areaStyle: blueArea('#93c5fd'),
              itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)'} }]
  });

  const quarterlyChart = echarts.init(document.getElementById('chart-quarterly'), 'blue');
  quarterlyChart.setOption({
    xAxis:{ type:'category', data: quarterlyLabels },
    yAxis:{ type:'value' },
    tooltip:{ trigger:'axis', formatter: p => `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명` },
    series:[{ type:'line', data:[100,180,240], areaStyle: blueArea('#93c5fd'),
              itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)'} }]
  });

  /* ── 연령/성별 ── */
  fetch('/api/ageStats')
    .then(res => res.json())
    .then(data => {
      echarts.init(document.getElementById('age-bar'), 'blue').setOption({
        xAxis:{ type:'category', data:Object.keys(data) },
        yAxis:{ type:'value' },
        tooltip:{ trigger:'axis' },
        series:[{ name:'가입자', type:'bar', data:Object.values(data),
          itemStyle: blueBar(), emphasis:{ itemStyle:{ shadowBlur:12, shadowColor:'rgba(37,99,235,.28)' } }
        }]
      });
    });

  echarts.init(document.getElementById('gender-pie'), 'blue').setOption({
    color:['#3b82f6','#93c5fd'],
    tooltip:{ trigger:'item' },
    legend:{ bottom:0 },
    series:[{ type:'pie', radius:'60%', data:[
      { value:1600, name:'남성' }, { value:1400, name:'여성' }
    ]}]
  });

  /* ── 리뷰 지표 ── */
  echarts.init(document.getElementById('review-count-bar'), 'blue').setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category', data: monthlyLabels },
    yAxis:{ type:'value' },
    series:[{ type:'bar', data:[18,32,26,44,38,51], itemStyle: blueBar(),
             emphasis:{ itemStyle:{ shadowBlur:12, shadowColor:'rgba(37,99,235,.28)' } } }]
  });

  echarts.init(document.getElementById('rating-line'), 'blue').setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category', data: monthlyLabels },
    yAxis:{ type:'value', max:5, min:0 },
    series:[{ type:'line', data:[4.1,4.2,4.0,4.3,4.4,4.5], areaStyle: blueArea('#93c5fd'),
             itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)' } }]
  });

  /* ── 누적 원화 & 공유 클릭 ── */
  echarts.init(document.getElementById('krw-area'), 'blue').setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category', data: quarterlyLabels },
    yAxis:{ type:'value', axisLabel: v => (v/1e8)+'억' },
    series:[{ type:'line', data:[5e8,8e8,1.3e9,1.9e9,2.5e9,3.2e9], areaStyle: blueArea('#93c5fd'),
             itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)' } }]
  });

  echarts.init(document.getElementById('share-gauge'), 'blue').setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category', data: dailyLabels },
    yAxis:{ type:'value' },
    series:[{ name:'클릭 수', type:'line', smooth:true, areaStyle: blueArea('#93c5fd'),
              data:[502,640,735,812,955,1023,982], itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)' } }]
  });

  /* ── 최근 리뷰 더미 테이블 ── */
  const reviews = [
    { date:'2025-08-01', nick:'kfx***',  rate:4.5, text:'환전 수수료가 낮아져서 만족합니다.' },
    { date:'2025-07-29', nick:'blue***', rate:3.5, text:'앱이 가끔 끊겨요. 개선되면 좋겠습니다.' },
    { date:'2025-07-20', nick:'hana***', rate:5.0, text:'해외 송금 속도가 빨라서 편해요!' }
  ];
  const tbody = document.getElementById('review-list');
  if (tbody) {
    reviews.forEach(r => {
      tbody.insertAdjacentHTML('beforeend', `
        <tr>
          <td>${r.date}</td>
          <td>${r.nick}</td>
          <td>${r.rate}</td>
          <td>${r.text}</td>
        </tr>`);
    });
  }
}); // DOMContentLoaded


/* ================== 2) 데이터 추출 & 리포트 빌드 유틸 ================== */
function extractSeriesDataByDomId(domId, seriesIndex = 0) {
  const el = document.getElementById(domId);
  if (!el) return null;
  const inst = echarts.getInstanceByDom(el);
  if (!inst) return null;
  const opt = inst.getOption() || {};
  const labels = (opt.xAxis && opt.xAxis[0] && opt.xAxis[0].data) ? opt.xAxis[0].data : [];
  const series = (opt.series && opt.series[seriesIndex] && opt.series[seriesIndex].data) ? opt.series[seriesIndex].data : [];
  return { labels, data: series };
}

async function buildStatsJson() {
  const subscribers = {
    daily: extractSeriesDataByDomId('chart-daily'),
    monthly: extractSeriesDataByDomId('chart-monthly'),
    quarterly: extractSeriesDataByDomId('chart-quarterly')
  };

  // 연령대 (API)
  let ageStats = null;
  try {
    const res = await fetch('/api/ageStats');
    if (res.ok) ageStats = await res.json();
  } catch (_) {}

  // 성별 (pie에서)
  let gender = null;
  const gInst = echarts.getInstanceByDom(document.getElementById('gender-pie'));
  if (gInst) {
    const gopt = gInst.getOption();
    const arr = (gopt.series && gopt.series[0] && gopt.series[0].data) ? gopt.series[0].data : [];
    gender = arr.reduce((acc, cur) => (acc[cur.name] = cur.value, acc), {});
  }

  const reviewsCountMonthly = extractSeriesDataByDomId('review-count-bar');
  const ratingMonthly      = extractSeriesDataByDomId('rating-line');

  let keywords = null;
  const wcEl = document.getElementById('sentiment-wordcloud');
  if (wcEl) {
    const wcInst = echarts.getInstanceByDom(wcEl);
    if (wcInst) {
      const wopt = wcInst.getOption();
      keywords = (wopt.series && wopt.series[0] && wopt.series[0].data) ? wopt.series[0].data : null;
    }
  }

  const krwCumulative    = extractSeriesDataByDomId('krw-area');
  const shareClicksDaily = extractSeriesDataByDomId('share-gauge');

  const recentReviews = Array.from(document.querySelectorAll('#review-list tr')).map(tr => {
    const tds = tr.querySelectorAll('td');
    return {
      date: tds[0]?.textContent?.trim(),
      nick: tds[1]?.textContent?.trim(),
      rate: tds[2]?.textContent?.trim(),
      text: tds[3]?.textContent?.trim(),
    };
  });

  return {
    generatedAt: new Date().toISOString(),
    subscribers,
    demographics: { age: ageStats, gender },
    reviews: { countMonthly: reviewsCountMonthly, ratingMonthly, recentReviews, keywords },
    usage: { krwCumulative, shareClicksDaily }
  };
}

/* ===== 마크다운 → HTML ===== */
function escapeHtml(s){ return (s || '').replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c])); }
function markdownToHtml(md){
  let html = escapeHtml((md||'').trim())
    .replace(/^######\s*(.+)$/gm, '<h6>$1</h6>')
    .replace(/^#####\s*(.+)$/gm, '<h5>$1</h5>')
    .replace(/^####\s*(.+)$/gm,  '<h4>$1</h4>')
    .replace(/^###\s*(.+)$/gm,   '<h3>$1</h3>')
    .replace(/^##\s*(.+)$/gm,    '<h2>$1</h2>')
    .replace(/^#\s*(.+)$/gm,     '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g,   '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g,       '<em>$1</em>')
    .replace(/^\s*-\s+(.+)$/gm,  '<li>$1</li>')
    .replace(/(<li>.*<\/li>\s*)+/g, m => `<ul>${m}</ul>`)
    .replace(/\n{2,}/g, '</p><p>')
    .replace(/\n/g, '<br/>');
  return `<p>${html}</p>`;
}

/* ===== 섹션 ↔ 차트 매핑 & 이미지 생성 ===== */
const CHART_META = {
  'chart-daily':        { label:'최근 7일 가입자수' },
  'chart-monthly':      { label:'최근 6개월 가입자수' },
  'chart-quarterly':    { label:'최근 6분기 가입자수' },
  'age-bar':            { label:'연령대 분포' },
  'gender-pie':         { label:'성별 비율' },
  'review-count-bar':   { label:'월별 리뷰 수' },
  'rating-line':        { label:'월별 평균 평점' },
  'sentiment-wordcloud':{ label:'키워드 워드클라우드' },
  'krw-area':           { label:'누적 원화 사용액' },
  'share-gauge':        { label:'공유 클릭 수(일별)' },
};

function getChartIdsForTitle(title='') {
  const t = title.toLowerCase();
  const ids = new Set();
  if (t.includes('가입자')) { ids.add('chart-daily'); ids.add('chart-monthly'); ids.add('chart-quarterly'); }
  if (t.includes('연령') || t.includes('성별')) { ids.add('age-bar'); ids.add('gender-pie'); }
  if (t.includes('리뷰') || t.includes('평점')) { ids.add('review-count-bar'); ids.add('rating-line'); }
  if (t.includes('키워드')) { ids.add('sentiment-wordcloud'); }
  if (t.includes('사용액') || t.includes('누적') || t.includes('원화')) { ids.add('krw-area'); }
  if (t.includes('공유') || t.includes('클릭')) { ids.add('share-gauge'); }
  return [...ids];
}

function getChartImageById(domId) {
  const el = document.getElementById(domId);
  if (!el) return null;
  const inst = echarts.getInstanceByDom(el);
  if (!inst) return null;
  try {
    return inst.getDataURL({ pixelRatio: 2, backgroundColor: '#fff' });
  } catch {
    return null;
  }
}

function parseReportSections(mdText='') {
  const parts = mdText.replace(/\r/g,'').split(/\n####\s*/);
  const sections = [];
  for (let i = 1; i < parts.length; i++) {
    const block = parts[i];
    const nl = block.indexOf('\n');
    const title = (nl === -1 ? block : block.slice(0, nl)).trim();
    const content = (nl === -1 ? '' : block.slice(nl + 1)).trim();
    sections.push({ title, content });
  }
  return sections.length ? sections : [{ title:'리포트', content: mdText }];
}

async function getChartsForSectionTitles(titles=[]) {
  const map = {};
  for (const title of titles) {
    const ids = getChartIdsForTitle(title);
    const images = ids.map(id => {
      const src = getChartImageById(id);
      return src ? { id, src, label: (CHART_META[id]?.label || id) } : null;
    }).filter(Boolean);
    map[title] = images;
  }
  return map;
}

function buildStructuredReportHtml(sections=[], chartsByTitle={}) {
  const css = `
  <style>
    .report-wrap{line-height:1.6}
    .report-sec{margin:18px 0 10px;padding-bottom:8px;border-bottom:1px dashed #e6efff}
    .report-sec:last-child{border-bottom:0}
    .report-sec h3{margin:0 0 8px;font-size:18px;color:#1e40af}
    .charts-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:6px 0 10px}
    .chart-card{border:1px solid rgba(37,99,235,.18);border-radius:10px;padding:10px;background:#fff;box-shadow:0 12px 28px rgba(37,99,235,.08), inset 0 1px 0 #fff}
    .chart-card img{width:100%;display:block;border-radius:6px}
    .chart-cap{margin-top:6px;font-size:12px;color:#64748b}
    .report-sec ul{margin:6px 0 10px 18px}
  </style>`;
  let html = `<div class="report-wrap">${css}`;
  for (const sec of sections) {
    const imgs = chartsByTitle[sec.title] || [];
    html += `<section class="report-sec"><h3>${sec.title}</h3>`;
    if (imgs.length){
      html += `<div class="charts-grid">` +
        imgs.map(im => `<figure class="chart-card"><img src="${im.src}" alt="${im.label}"><figcaption class="chart-cap">${im.label}</figcaption></figure>`).join('') +
        `</div>`;
    }
    html += markdownToHtml(sec.content) + `</section>`;
  }
  html += `</div>`;
  return html;
}


/* ================== 3) 모달 & 버튼 동작 + 동의 게이트 ================== */
const generateBtn = document.getElementById('btn-generate-report');
const statusEl    = document.getElementById('report-status');

let lastReportText = '';
let lastReportHtml = '';
let lastSections = [];
let lastChartsByTitle = {};
let lastStatsJson  = null;

function openReportModal(html){
  const modal = document.getElementById('ai-report-modal');
  const body  = document.getElementById('ai-report-body');
  if (!modal || !body) return;
  body.innerHTML = html || '(빈 리포트)';
  modal.style.display = 'block';
  document.body.style.overflow = 'hidden';
}
function closeReportModal(){
  const modal = document.getElementById('ai-report-modal');
  if (!modal) return;
  modal.style.display = 'none';
  document.body.style.overflow = '';
}
document.getElementById('ai-modal-close')?.addEventListener('click', closeReportModal);
document.querySelector('#ai-report-modal .ai-modal__backdrop')?.addEventListener('click', closeReportModal);
window.addEventListener('keydown', e => { if (e.key === 'Escape') closeReportModal(); });

/* ===== 동의 모달 로직 ===== */
const CONSENT_KEY = 'dashboard_report_consent_v1';
let __onConsentOk = null;

function openConsentModal(startCb){
  __onConsentOk = startCb;
  const m = document.getElementById('consent-modal');
  if (!m) { // 모달이 없으면 바로 진행
    startReportAnalysis(null);
    return;
  }
  m.style.display = 'block';
  document.body.style.overflow = 'hidden';
  document.getElementById('consent-form')?.reset();
  const confirmBtn = document.getElementById('consent-confirm');
  if (confirmBtn) confirmBtn.disabled = true;
}
function closeConsentModal(){
  const m = document.getElementById('consent-modal');
  if (!m) return;
  m.style.display = 'none';
  document.body.style.overflow = '';
}

document.getElementById('consent-close')?.addEventListener('click', closeConsentModal);
document.getElementById('consent-cancel')?.addEventListener('click', closeConsentModal);

// 입력 유효성 → 버튼 활성화
['consent-name','consent-birth','consent-purpose','consent-agree'].forEach(id=>{
  document.getElementById(id)?.addEventListener('input', ()=>{
    const ok = (document.getElementById('consent-name')?.value || '').trim() &&
               (document.getElementById('consent-birth')?.value || '') &&
               (document.getElementById('consent-purpose')?.value || '').trim() &&
               (document.getElementById('consent-agree')?.checked || false);
    const btn = document.getElementById('consent-confirm');
    if (btn) btn.disabled = !ok;
  });
});

// 동의 후 진행
document.getElementById('consent-confirm')?.addEventListener('click', ()=>{
  const form = document.getElementById('consent-form');
  if (form && !form.checkValidity()) { form.reportValidity(); return; }

  const consent = {
    name: (document.getElementById('consent-name')?.value || '').trim(),
    birth: document.getElementById('consent-birth')?.value || '',
    purpose: (document.getElementById('consent-purpose')?.value || '').trim(),
    agreed: !!document.getElementById('consent-agree')?.checked,
    ts: new Date().toISOString()
  };
  closeConsentModal();
  __onConsentOk && __onConsentOk(consent);
});

/* ===== 리포트 분석 시작(기존 클릭 로직을 함수화) ===== */
async function startReportAnalysis(consent){
  try {
    if (!generateBtn) return;
    generateBtn.disabled = true;
    const oldText = generateBtn.textContent;
    generateBtn.textContent = '분석 중…';
    if (statusEl) statusEl.textContent = '분석 중…';

    const stats = await buildStatsJson();
    lastStatsJson = stats;

    // 백엔드에 동의정보를 보내고 싶지 않다면 아래 한 줄은 제거 ※
    const payload = { ...stats};

    const res = await fetch('/api/generateReport', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    lastReportText = (await res.text()) || '비어 있는 응답입니다.';

    const sections = parseReportSections(lastReportText);
    const chartsByTitle = await getChartsForSectionTitles(sections.map(s => s.title));
    lastSections = sections;
    lastChartsByTitle = chartsByTitle;
    lastReportHtml = buildStructuredReportHtml(sections, chartsByTitle);

    if (statusEl) statusEl.textContent = '완료';
    openReportModal(lastReportHtml);

    setTimeout(() => { if (statusEl) statusEl.textContent = ''; }, 1000);
    generateBtn.textContent = oldText;
    generateBtn.disabled = false;

  } catch (e) {
    if (statusEl) statusEl.textContent = '실패';
    openReportModal(markdownToHtml('리포트 생성 실패: ' + (e?.message || e)));
    if (generateBtn) {
      generateBtn.textContent = '리포트 분석';
      generateBtn.disabled = false;
    }
  }
}

/* ===== 버튼 클릭 → 동의 모달 게이트 ===== */
generateBtn?.addEventListener('click', () => {
  openConsentModal(startReportAnalysis);
});


/* ================== 4) 인쇄 / 엑셀 / 새 창 ================== */
// 인쇄(PDF)
document.getElementById('ai-modal-print')?.addEventListener('click', () => {
  if (!lastReportHtml) { alert('먼저 리포트를 생성하세요.'); return; }
  const html = `
  <html><head><meta charset="utf-8"><title>AI 리포트</title>
  <style>body{font-family:Arial,Helvetica,sans-serif;margin:24px}</style></head>
  <body>${lastReportHtml}
  <script>window.onload=()=>setTimeout(()=>print(),200)</script></body></html>`;
  const w = window.open('about:blank','_blank'); w.document.write(html); w.document.close();
});

/* ====== XLSX(표 레이아웃) 내보내기 — 차트 제외, 분석요약+데이터 표 (MD 제거) ====== */
document.getElementById('ai-modal-excel')?.addEventListener('click', async () => {
  if (!lastStatsJson) { alert('먼저 리포트를 생성하세요.'); return; }

  // --- 마크다운 제거 헬퍼 ---
  const stripMdTitleText = (s='') =>
    s.replace(/^#{1,6}\s*/, '').replace(/[*_`~]+/g, '').trim();

  const mdToPlainLines = (md='') => {
    let s = md;
    s = s.replace(/```[\s\S]*?```/g, '');
    s = s.replace(/^>+\s?/gm, '');
    s = s.replace(/!\[[^\]]*]\([^)]+\)/g, '');
    s = s.replace(/\[([^\]]+)]\([^)]+\)/g, '$1');
    s = s.replace(/\*\*(.*?)\*\*/g, '$1')
         .replace(/__(.*?)__/g, '$1')
         .replace(/\*(.*?)\*/g, '$1')
         .replace(/_(.*?)_/g, '$1')
         .replace(/`([^`]+)`/g, '$1');
    s = s.replace(/^#{1,6}\s*/gm, '');
    s = s.replace(/^\s*[-*+]\s+/gm, '• ');
    s = s.replace(/^\s*\d+\.\s+/gm, '• ');
    return s.split('\n').map(t => t.trim()).filter(Boolean);
  };

  if (window.ExcelJS) {
    const wb = new ExcelJS.Workbook();
    wb.created = new Date();

    // 시트1: 요약
    const wsSum = wb.addWorksheet('요약', { views:[{ showGridLines:false }] });
    wsSum.columns = [{width:4},{width:28},{width:80},{width:8}];
    wsSum.mergeCells(2,2,2,3);
    const h = wsSum.getCell(2,2);
    h.value = 'AI 리포트 분석 요약';
    h.font = { name:'Segoe UI', size:20, bold:true, color:{argb:'FF1E3A8A'} };
    h.alignment = { vertical:'middle' };
    wsSum.getRow(2).height = 26;

    let r = 4;
    const secs = lastSections?.length ? lastSections : parseReportSections(lastReportText);
    secs.forEach(sec => {
      wsSum.mergeCells(r,2,r,3);
      const th = wsSum.getCell(r,2);
      th.value = stripMdTitleText(sec.title || '섹션');
      th.font = { bold:true, size:14, color:{argb:'FF1E3A8A'} };
      th.fill = { type:'pattern', pattern:'solid', fgColor:{argb:'FFEFF6FF'} };
      r++;
      const lines = mdToPlainLines(sec.content || '');
      if (lines.length === 0) { r++; return; }
      lines.forEach(line => {
        wsSum.mergeCells(r,2,r,3);
        const c = wsSum.getCell(r,2);
        c.value = line;
        c.alignment = { wrapText:true };
        r++;
      });
      r += 1;
    });

    // 시트2: 데이터
    const ws = wb.addWorksheet('데이터', { views:[{ showGridLines:false, state:'frozen', ySplit:1 }] });
    ws.columns = [{width:24},{width:18},{width:10}];

    const addTitle = (row, text) => {
      ws.mergeCells(row,1,row,3);
      const c = ws.getCell(row,1);
      c.value = text;
      c.font = { bold:true, size:13, color:{argb:'FF1E3A8A'} };
      c.fill = { type:'pattern', pattern:'solid', fgColor:{argb:'FFEFF6FF'} };
      ws.getRow(row).height = 20;
      return row + 1;
    };

    let cur = 2;
    const addTable = (name, rows, unit, numFmt) => {
      if (!rows || rows.length === 0) return;
      cur = addTitle(cur, `${name} (단위: ${unit})`);
      const ref = `A${cur}`;
      ws.addTable({
        name: `T_${name.replace(/\W+/g,'_')}_${cur}`,
        ref,
        headerRow: true,
        style: { theme: 'TableStyleMedium9', showRowStripes: true },
        columns: [{name:'라벨'},{name:'값'},{name:'단위'}],
        rows: rows.map(([lab, val]) => [lab, (val ?? ''), unit])
      });
      const start = cur + 1, end = cur + rows.length;
      for (let rr = start; rr <= end; rr++) ws.getCell(rr, 2).numFmt = numFmt;
      cur = end + 3;
    };

    const toPairs = obj => (obj?.labels && obj?.data)
      ? obj.labels.map((lab,i)=> [lab, Number(obj.data[i])])
      : [];

    addTable('가입자-일별',   toPairs(lastStatsJson.subscribers?.daily),     '명', '#,##0');
    addTable('가입자-월별',   toPairs(lastStatsJson.subscribers?.monthly),   '명', '#,##0');
    addTable('가입자-분기별', toPairs(lastStatsJson.subscribers?.quarterly), '명', '#,##0');

    if (lastStatsJson.demographics?.age) {
      addTable('연령대', Object.entries(lastStatsJson.demographics.age).map(([k,v]) => [k, Number(v)]), '명', '#,##0');
    }
    if (lastStatsJson.demographics?.gender) {
      addTable('성별', Object.entries(lastStatsJson.demographics.gender).map(([k,v]) => [k, Number(v)]), '명', '#,##0');
    }

    addTable('리뷰-월별갯수', toPairs(lastStatsJson.reviews?.countMonthly),   '건', '#,##0');
    addTable('평점-월별평균', toPairs(lastStatsJson.reviews?.ratingMonthly), '점', '0.0');
    addTable('누적 원화 사용액', toPairs(lastStatsJson.usage?.krwCumulative), '원', '#,##0');
    addTable('공유 클릭(일별)', toPairs(lastStatsJson.usage?.shareClicksDaily), '회', '#,##0');

    const buf = await wb.xlsx.writeBuffer();
    const blob = new Blob([buf], { type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `AI_리포트_표_${new Date().toISOString().slice(0,10)}.xlsx`;
    a.click();
    setTimeout(()=> URL.revokeObjectURL(a.href), 1000);
    return;
  }

  // ExcelJS 없으면 CSV
  const rows = [];
  rows.push(['섹션','라벨','값','단위']);
  const pushSeries = (section, obj, unit) => {
    if (!obj?.labels || !obj?.data) return;
    obj.labels.forEach((lab,i)=> rows.push([section, lab, obj.data[i], unit]));
  };
  pushSeries('가입자-일별',   lastStatsJson.subscribers?.daily,     '명');
  pushSeries('가입자-월별',   lastStatsJson.subscribers?.monthly,   '명');
  pushSeries('가입자-분기별', lastStatsJson.subscribers?.quarterly, '명');
  if (lastStatsJson.demographics?.age)
    Object.entries(lastStatsJson.demographics.age).forEach(([k,v])=> rows.push(['연령대', k, v, '명']));
  if (lastStatsJson.demographics?.gender)
    Object.entries(lastStatsJson.demographics.gender).forEach(([k,v])=> rows.push(['성별', k, v, '명']));
  pushSeries('리뷰-월별갯수', lastStatsJson.reviews?.countMonthly, '건');
  pushSeries('평점-월별평균', lastStatsJson.reviews?.ratingMonthly, '점');
  pushSeries('누적원화',      lastStatsJson.usage?.krwCumulative,  '원');
  pushSeries('공유클릭-일별', lastStatsJson.usage?.shareClicksDaily,'회');

  const csv = '\uFEFF' + rows.map(r => r.map(v => `"${(v ?? '').toString().replace(/"/g,'""')}"`).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const a    = document.createElement('a');
  a.href = url; a.download = `dashboard-report_${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
  setTimeout(()=> URL.revokeObjectURL(url), 1000);
});

// 새 창
document.getElementById('ai-modal-popout')?.addEventListener('click', () => {
  if (!lastReportHtml) { alert('먼저 리포트를 생성하세요.'); return; }
  const w = window.open('about:blank','_blank');
  w.document.write(`<html><head><meta charset="utf-8"><title>AI 리포트</title></head><body style="font-family:Arial,Helvetica,sans-serif;margin:24px">${lastReportHtml}</body></html>`);
  w.document.close();
});
