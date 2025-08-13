// static/js/echarts-dashboard.js

/* ================== 0) ECharts 파란 테마 ================== */
(function registerBlueTheme(){
  if (typeof echarts === 'undefined') return;
  const BLUE = {
    color:['#3b82f6','#60a5fa','#93c5fd','#1d4ed8','#38bdf8','#818cf8'],
    backgroundColor:'transparent',
    textStyle:{color:'#0f172a'},
    legend:{textStyle:{color:'#334155'}},
    tooltip:{
      backgroundColor:'#fff',borderColor:'#dbeafe',borderWidth:1,
      textStyle:{color:'#0f172a'},shadowBlur:8,shadowColor:'rgba(37,99,235,.12)'
    },
    axisPointer:{lineStyle:{color:'#3b82f6'}},
    grid:{left:30,right:20,top:32,bottom:28,containLabel:true},
    categoryAxis:{
      axisLine:{lineStyle:{color:'#9db2d7'}},axisTick:{show:false},
      axisLabel:{color:'#476082'},splitLine:{show:false}
    },
    valueAxis:{
      axisLine:{show:false},axisTick:{show:false},
      axisLabel:{color:'#476082'},splitLine:{show:true,lineStyle:{color:'#eaf1ff'}}
    },
    line:{lineStyle:{width:3},symbol:'circle',symbolSize:6,smooth:true},
    bar:{itemStyle:{borderRadius:[8,8,0,0]}},
    pie:{itemStyle:{borderColor:'#fff',borderWidth:2},label:{color:'#0f172a'}}
  };
  echarts.registerTheme('blue', BLUE);
})();
const G = echarts.graphic;
const blueArea = (top='#93c5fd') => ({
  color:new G.LinearGradient(0,0,0,1,[{offset:0,color:top},{offset:1,color:'rgba(147,197,253,0.06)'}])
});
const blueBar  = () => ({
  color:new G.LinearGradient(0,0,0,1,[{offset:0,color:'#60a5fa'},{offset:1,color:'#3b82f6'}]),
  shadowBlur:8,shadowColor:'rgba(37,99,235,.18)',shadowOffsetY:4,borderRadius:[8,8,0,0]
});

function getOrInitChart(id){
  const el=document.getElementById(id);
  if(!el) return null;
  return echarts.getInstanceByDom(el)||echarts.init(el,'blue');
}
const maskUid = s=>{s=String(s||'');return s.length<=3?s+'***':s.slice(0,3)+'***';};

/* ================== 1) 차트 렌더 ================== */
document.addEventListener('DOMContentLoaded', () => {
  const today=new Date();
  const lastNDays=n=>Array.from({length:n},(_,i)=>{const d=new Date(today);d.setDate(today.getDate()-(n-1-i));return `${d.getMonth()+1}/${d.getDate()}`;});
  const lastNQuarters=n=>Array.from({length:n},(_,i)=>{const d=new Date(today);d.setMonth(today.getMonth()-3*(n-1-i));const q=Math.floor(d.getMonth()/3)+1;return `${d.getFullYear()} Q${q}`;});
  const dailyLabels=lastNDays(7), quarterlyLabels=lastNQuarters(6);

  // 가입자수
  getOrInitChart('chart-daily')?.setOption({
    xAxis:{type:'category',data:dailyLabels}, yAxis:{type:'value'},
    tooltip:{trigger:'axis',formatter:p=>`${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`},
    series:[{type:'line',data:[5,9,6,7,10,8,12],areaStyle:blueArea(),itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
  });
  getOrInitChart('chart-monthly')?.setOption({
    xAxis:{type:'category',data:['3월','4월','5월','6월','7월','8월']}, yAxis:{type:'value'},
    tooltip:{trigger:'axis',formatter:p=>`${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`},
    series:[{type:'line',data:[30,50,60,80,120,140],areaStyle:blueArea(),itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
  });
  getOrInitChart('chart-quarterly')?.setOption({
    xAxis:{type:'category',data:quarterlyLabels}, yAxis:{type:'value'},
    tooltip:{trigger:'axis',formatter:p=>`${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`},
    series:[{type:'line',data:[100,180,240,300,380,460],areaStyle:blueArea(),itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
  });

  // 연령/성별
  fetch('/api/ageStats').then(r=>r.json()).then(data=>{
    getOrInitChart('age-bar')?.setOption({
      xAxis:{type:'category',data:Object.keys(data)}, yAxis:{type:'value'}, tooltip:{trigger:'axis'},
      series:[{type:'bar',data:Object.values(data),itemStyle:blueBar(),emphasis:{itemStyle:{shadowBlur:12,shadowColor:'rgba(37,99,235,.28)'}}}]
    });
  }).catch(()=>{});
  // 성별 비율 
  fetch('/api/genderStats')
    .then(r => r.json())
    .then(obj => {
      const pieData = [
        { name: '남성', value: Number(obj['남성'] ?? obj.male ?? 0) },
        { name: '여성', value: Number(obj['여성'] ?? obj.female ?? 0) }
      ];
      const other = Number(obj['기타'] ?? obj.other ?? 0);
      if (other > 0) pieData.push({ name: '기타', value: other });

      // (선택) 리포트 빌드 폴백용 전역 백업
      window.__genderPie = pieData;

      getOrInitChart('gender-pie')?.setOption({
        color:['#3b82f6','#93c5fd','#cbd5e1'],
        legend:{bottom:0},
        tooltip:{trigger:'item'},
        series:[{ type:'pie', radius:'60%', data: pieData }]
      });
    })
    .catch(() => {
      // 실패 시 빈 파이로 유지
      getOrInitChart('gender-pie')?.setOption({
        legend:{bottom:0}, series:[{ type:'pie', radius:'60%', data: [] }]
      });
    });

  // 리뷰 통계
  (async function renderReviewCharts(){
    try{
      const r=await fetch('/api/reviews/statsMonthly?months=6');
      const {labels,counts,avgRatings}=await r.json();
      getOrInitChart('review-count-bar')?.setOption({
        tooltip:{trigger:'axis'}, xAxis:{type:'category',data:labels}, yAxis:{type:'value'},
        series:[{type:'bar',data:counts,itemStyle:blueBar(),emphasis:{itemStyle:{shadowBlur:12,shadowColor:'rgba(37,99,235,.28)'}}}]
      });
      getOrInitChart('rating-line')?.setOption({
        tooltip:{trigger:'axis'}, xAxis:{type:'category',data:labels}, yAxis:{type:'value',max:5,min:0},
        series:[{type:'line',data:avgRatings,areaStyle:blueArea(),itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
      });
    }catch(e){console.warn('리뷰 월별 통계 로드 실패',e);}
  })();

  // 누적 사용액
  getOrInitChart('krw-area')?.setOption({
    tooltip:{trigger:'axis'}, xAxis:{type:'category',data:quarterlyLabels},
    yAxis:{type:'value',axisLabel:v=>(v/1e8)+'억'},
    series:[{type:'line',data:[5e8,8e8,1.3e9,1.9e9,2.5e9,3.2e9],areaStyle:blueArea(),itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
  });

  // 고정 라벨(정렬 보장)
  const CUR_ORDER   = ['USD','JPY','CNH','EUR','CHF','VND'];
  const TOPIC_ORDER = ['TRAVEL','STUDY','SHOPPING','FINANCE','ETC'];

  // 관심 통화 (도넛 파이)
  fetch('/api/interests/currencies')
    .then(r => r.json())
    .then(data => {
      const seriesData = CUR_ORDER.map(k => ({ name: k, value: Number(data?.[k] || 0) }));
      getOrInitChart('currency-pie')?.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 0, top: 'middle' },  // 전설을 우측으로
        series: [{
          type: 'pie',
          radius: ['45%','70%'],
          center: ['40%','50%'],          // 전설 공간 확보(도넛을 왼쪽으로)
          top: 8, bottom: 8,               // 상하 여백
          avoidLabelOverlap: true,
          minAngle: 6,                     // 작은 조각 라벨 겹침 방지
          label: {
            show: true,
            formatter: '{b}: {c}',
            fontSize: 12,
            overflow: 'truncate',
            ellipsis: '…'
          },
          labelLine: { length: 8, length2: 6 }, // 라벨선 짧게
          data: seriesData
        }]
      });
    })
    .catch(()=>{});

  // 관심 분야 (가로 막대)
  fetch('/api/interests/topics')
    .then(r => r.json())
    .then(data => {
      const labels = TOPIC_ORDER;
      const values = labels.map(k => Number(data?.[k] || 0));
      getOrInitChart('interest-bar')?.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 20, bottom: 28, containLabel: true },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: labels },
        series: [{
          type: 'bar', data: values, itemStyle: blueBar(),
          emphasis: { itemStyle: { shadowBlur: 12, shadowColor:'rgba(37,99,235,.28)' } }
        }]
      });
    })
    .catch(()=>{});

  // ★ 워드클라우드 (좌/우 2개)
  (async function renderWordcloud(){
    const posChart = getOrInitChart('kw-pos');
    const negChart = getOrInitChart('kw-neg');
    if (!posChart && !negChart) return;

    try {
      const r = await fetch('/api/reviews/keywords?months=3');
      const { positive = [], negative = [] } = await r.json();

      // === 정확히 15개만 사용 ===
      const pickTop = (arr, n = 15) =>
        [...arr].sort((a,b)=>(b.value||1)-(a.value||1)).slice(0, n);

      const toSeriesData = (arr, color) =>
        pickTop(arr, 15)
          .filter(k => k && k.name && (k.value ?? 1) > 0)
          .map(k => ({ name: k.name, value: k.value || 1, textStyle: { color } }));

      // === 자리 부족 방지: 글자/격자 축소 ===
      const base = (data, fontFamily) => ({
        type: 'wordCloud',
        left: '6%', right: '6%', top: '8%', bottom: '8%', // 여백 증가
        gridSize: 20,            // 간격 키움 (겹침 방지)
        sizeRange: [12, 24],     // 글자 크기 낮춤
        rotationRange: [0, 0],
        layoutAnimation: false,
        shape: 'circle',
        drawOutOfBound: false,
        textStyle: { fontFamily },
        emphasis: { focus: 'self', textStyle: { fontWeight: 900 } },
        data
      });
      if (posChart) {
        posChart.setOption({
          tooltip: {},
          series: [ base(toSeriesData(positive, '#2563eb'), 'SUIT Variable, Pretendard, system-ui, sans-serif') ]
        });
      }
      if (negChart) {
        negChart.setOption({
          tooltip: {},
          series: [ base(toSeriesData(negative, '#ef4444'), 'Pretendard, SUIT Variable, system-ui, sans-serif') ]
        });
      }

      // 둘 다 데이터 없으면 메시지
      if ((!positive.length) && (!negative.length)) {
        const setEmpty = (inst) => {
          const dom = inst?.getDom?.(); if (!dom) return;
          dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#64748b">표시할 키워드가 없습니다.</div>';
        };
        setEmpty(posChart); setEmpty(negChart);
      }
    } catch (e) {
      console.warn('워드클라우드 로드 실패', e);
      [posChart, negChart].forEach(ch => {
        const dom = ch?.getDom?.(); if (!dom) return;
        dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#64748b">키워드를 가져오지 못했습니다.</div>';
      });
    }
  })();

  // 최근 리뷰 테이블
  (async function renderRecentReviews(){
    const tbody = document.getElementById('review-list');
    if (!tbody) return;
    try {
      const res  = await fetch('/api/reviews/recent?limit=20');
      const data = await res.json();
      const items = Array.isArray(data) ? data : (data.items || []);
      tbody.innerHTML = items.length
        ? items.map(r => {
            const date = r.rvdate ?? r.rvDate ?? r.date ?? r.createdAt ?? '';
            const nick = r.nickname ?? r.nick ?? r.userName ?? r.uid ?? '';
            const rate = r.rating ?? r.rvscore ?? r.score ?? r.rvScore ?? '';
            const text = r.content ?? r.rvcontent ?? r.review ?? r.text ?? '';
            const masked = (nick && nick.length > 3) ? (nick.slice(0,3) + '***') : (nick + '***');
            return `<tr><td>${date}</td><td>${masked}</td><td>${rate}</td><td>${text}</td></tr>`;
          }).join('')
        : `<tr><td colspan="4">최근 리뷰가 없습니다.</td></tr>`;
    } catch (e) {
      console.warn('최근 리뷰 로드 실패', e);
      tbody.innerHTML = `<tr><td colspan="4">최근 리뷰를 불러오지 못했습니다.</td></tr>`;
    }
  })();

  // === 공통 리사이즈 (전체 차트) ===
  function resizeCharts(ids){
    ids.forEach(id=>{
      const el = document.getElementById(id);
      const inst = el && echarts.getInstanceByDom(el);
      inst && inst.resize();
    });
  }
  const ALL_CHART_IDS = [
    'chart-daily','chart-monthly','chart-quarterly',
    'age-bar','gender-pie',
    'review-count-bar','rating-line',
    'krw-area','currency-pie','interest-bar',
    'kw-pos','kw-neg'
  ];
  setTimeout(()=>resizeCharts(ALL_CHART_IDS), 0);
  window.addEventListener('resize', ()=>resizeCharts(ALL_CHART_IDS));

  /* ================== 2) 리포트 생성 공통 유틸 ================== */
  function extractSeriesDataByDomId(domId, seriesIndex = 0) {
    const el = document.getElementById(domId); if (!el) return null;
    const inst = echarts.getInstanceByDom(el); if (!inst) return null;
    const opt = inst.getOption() || {};

    const s = (opt.series && opt.series[seriesIndex]) ? opt.series[seriesIndex] : null;
    let rawData = Array.isArray(s?.data) ? s.data : [];

    // 라벨: xAxis->yAxis 순서로 시도, 없으면 series의 name 사용
    let labels = [];
    if (opt.xAxis?.[0]?.data?.length) labels = opt.xAxis[0].data;
    else if (opt.yAxis?.[0]?.data?.length) labels = opt.yAxis[0].data;
    else if (rawData.length && typeof rawData[0] === 'object' && 'name' in rawData[0]) {
      labels = rawData.map(d => d.name);
    }

    // 값: 객체면 value, 아니면 원시값
    const data = rawData.map(v => (typeof v === 'object' && v !== null && 'value' in v) ? v.value : v);

    return { labels, data };
  }
  function extractPieAsObject(domId){
    const el = document.getElementById(domId);
    const inst = el && echarts.getInstanceByDom(el);
    if (!inst) return null;
    const data = inst.getOption()?.series?.[0]?.data || [];
    const out = {};
    data.forEach(d => out[d.name] = d.value);
    return out;
  }

  async function buildStatsJson() {
    let reviewsCountMonthly = extractSeriesDataByDomId('review-count-bar');
    let ratingMonthly       = extractSeriesDataByDomId('rating-line');

    if (!reviewsCountMonthly?.labels?.length || !ratingMonthly?.labels?.length) {
      try {
        const res = await fetch('/api/reviews/statsMonthly?months=6');
        const { labels, counts, avgRatings } = await res.json();
        if (!reviewsCountMonthly?.labels?.length) reviewsCountMonthly = { labels, data: counts };
        if (!ratingMonthly?.labels?.length)       ratingMonthly       = { labels, data: avgRatings };
      } catch(_) {}
    }

    const subscribers = {
      daily:     extractSeriesDataByDomId('chart-daily'),
      monthly:   extractSeriesDataByDomId('chart-monthly'),
      quarterly: extractSeriesDataByDomId('chart-quarterly')
    };

    // 연령/성별
    let ageStats = null;
    try { const res = await fetch('/api/ageStats'); if (res.ok) ageStats = await res.json(); } catch (_) {}
    let gender = null;
    const gInst = echarts.getInstanceByDom(document.getElementById('gender-pie'));
    if (gInst) {
      const arr = (gInst.getOption().series?.[0]?.data) || [];
      gender = arr.reduce((acc, cur) => (acc[cur.name] = cur.value, acc), {});
    }

    // 누적 사용액
    const krwCumulative = extractSeriesDataByDomId('krw-area');

    // 관심 지표
    const currencyDist = extractPieAsObject('currency-pie');      // { USD: n, ... }
    const topicSeries  = extractSeriesDataByDomId('interest-bar'); // { labels:[...], data:[...] }
    const topicMap = (() => {
      const m = {};
      if (topicSeries?.labels?.length) {
        topicSeries.labels.forEach((lab, i) => m[lab] = Number(topicSeries.data?.[i] || 0));
      }
      return m;
    })();

    // 최근 리뷰 테이블
    const recentReviews = Array.from(document.querySelectorAll('#review-list tr')).map(tr => {
      const tds = tr.querySelectorAll('td');
      return { date: tds[0]?.textContent?.trim(), nick: tds[1]?.textContent?.trim(),
               rate: tds[2]?.textContent?.trim(), text: tds[3]?.textContent?.trim() };
    });

    return {
      generatedAt: new Date().toISOString(),
      subscribers,
      demographics: { age: ageStats, gender },
      reviews: { countMonthly: reviewsCountMonthly, ratingMonthly, recentReviews, keywords: null },
      usage: { krwCumulative },
      affinity: {
        currency: currencyDist,
        topic: { labels: topicSeries?.labels || [], data: topicSeries?.data || [] },
        topicMap // { TRAVEL: n, STUDY: n, ... }
      }
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

  /* ===== 섹션 ↔ 차트 매핑(리포트 이미지용) ===== */
  const CHART_META = {
    'chart-daily':      { label:'최근 7일 가입자수' },
    'chart-monthly':    { label:'최근 6개월 가입자수' },
    'chart-quarterly':  { label:'최근 6분기 가입자수' },
    'age-bar':          { label:'연령대 분포' },
    'gender-pie':       { label:'성별 비율' },
    'review-count-bar': { label:'월별 리뷰 수' },
    'rating-line':      { label:'월별 평균 평점' },
    'krw-area':         { label:'누적 원화 사용액' },
    'currency-pie':     { label:'관심 통화 분포' },
    'interest-bar':     { label:'관심 분야 분포' },
  };
  function getChartIdsForTitle(title='') {
    const t = title.toLowerCase();
    const ids = new Set();
    if (t.includes('가입자')) { ids.add('chart-daily'); ids.add('chart-monthly'); ids.add('chart-quarterly'); }
    if (t.includes('연령') || t.includes('성별')) { ids.add('age-bar'); ids.add('gender-pie'); }
    if (t.includes('리뷰') || t.includes('평점')) { ids.add('review-count-bar'); ids.add('rating-line'); }
    if (t.includes('사용액') || t.includes('누적') || t.includes('원화')) { ids.add('krw-area'); }
    if (t.includes('관심') || t.includes('통화') || t.includes('분야')) { ids.add('currency-pie'); ids.add('interest-bar'); }
    return [...ids];
  }
  function getChartImageById(domId){
    const el=document.getElementById(domId); if(!el) return null;
    const inst=echarts.getInstanceByDom(el); if(!inst) return null;
    try{ return inst.getDataURL({ pixelRatio:2, backgroundColor:'#fff' }); }catch{ return null; }
  }
  function parseReportSections(mdText=''){
    const parts = mdText.replace(/\r/g,'').split(/\n####\s*/); const sections=[];
    for(let i=1;i<parts.length;i++){
      const block=parts[i]; const nl=block.indexOf('\n');
      const title=(nl===-1?block:block.slice(0,nl)).trim();
      const content=(nl===-1?'':block.slice(nl+1)).trim();
      sections.push({ title, content });
    }
    return sections.length?sections:[{ title:'리포트', content: mdText }];
  }
  async function getChartsForSectionTitles(titles=[]){
    const map={};
    for(const title of titles){
      const ids=getChartIdsForTitle(title);
      map[title]=ids.map(id=>{
        const src=getChartImageById(id);
        return src?{id,src,label:(CHART_META[id]?.label||id)}:null;
      }).filter(Boolean);
    }
    return map;
  }
  function buildStructuredReportHtml(sections=[], chartsByTitle={}){
    const css=`<style>.report-wrap{line-height:1.6}.report-sec{margin:18px 0 10px;padding-bottom:8px;border-bottom:1px dashed #e6efff}
    .report-sec:last-child{border-bottom:0}.report-sec h3{margin:0 0 8px;font-size:18px;color:#1e40af}
    .charts-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:6px 0 10px}
    .chart-card{border:1px solid rgba(37,99,235,.18);border-radius:10px;padding:10px;background:#fff;box-shadow:0 12px 28px rgba(37,99,235,.08), inset 0 1px 0 #fff}
    .chart-card img{width:100%;display:block;border-radius:6px}.chart-cap{margin-top:6px;font-size:12px;color:#64748b}
    .report-sec ul{margin:6px 0 10px 18px}</style>`;
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
    return html + `</div>`;
  }

  /* ================== 3) 모달 & 리포트 버튼 ================== */
  const generateBtn = document.getElementById('btn-generate-report');
  const statusEl    = document.getElementById('report-status');
  let lastReportText='', lastReportHtml='', lastSections=[], lastChartsByTitle={}, lastStatsJson=null;

  function openReportModal(html){
    const modal=document.getElementById('ai-report-modal');
    const body=document.getElementById('ai-report-body');
    if(!modal||!body) return;
    body.innerHTML=html||'(빈 리포트)';
    modal.style.display='block';
    document.body.style.overflow='hidden';
    bindReportActions(); // 모달 열릴 때마다 버튼 바인딩 보강
  }
  function closeReportModal(){
    const m=document.getElementById('ai-report-modal');
    if(!m) return;
    m.style.display='none'; document.body.style.overflow='';
  }
  document.getElementById('ai-modal-close')?.addEventListener('click', closeReportModal);
  document.querySelector('#ai-report-modal .ai-modal__backdrop')?.addEventListener('click', closeReportModal);
  window.addEventListener('keydown', e=>{ if(e.key==='Escape') closeReportModal(); });

  async function startReportAnalysis(){
    try{
      if(!generateBtn) return;
      generateBtn.disabled=true;
      const old=generateBtn.textContent;
      generateBtn.textContent='분석 중…';
      if(statusEl) statusEl.textContent='분석 중…';

      const stats=await buildStatsJson(); lastStatsJson=stats;
      const res=await fetch('/api/generateReport',{
        method:'POST', headers:{'Content-Type':'application/json'},
        body:JSON.stringify(stats)
      });
      lastReportText=(await res.text()) || '비어 있는 응답입니다.';
      const sections=parseReportSections(lastReportText);
      const chartsByTitle=await getChartsForSectionTitles(sections.map(s=>s.title));
      lastSections=sections; lastChartsByTitle=chartsByTitle; lastReportHtml=buildStructuredReportHtml(sections, chartsByTitle);

      if(statusEl) statusEl.textContent='완료';
      openReportModal(lastReportHtml);
      setTimeout(()=>{ if(statusEl) statusEl.textContent=''; }, 1000);
      generateBtn.textContent=old; generateBtn.disabled=false;
    }catch(e){
      if(statusEl) statusEl.textContent='실패';
      openReportModal(markdownToHtml('리포트 생성 실패: ' + (e?.message || e)));
      if(generateBtn){ generateBtn.textContent='리포트 분석'; generateBtn.disabled=false; }
    }
  }

  /* ================== 동의 모달 (매번 요구) ================== */
  const consentModal  = document.getElementById('consent-modal');
  const consentForm   = document.getElementById('consent-form');
  const nameEl        = document.getElementById('consent-name');
  const birthEl       = document.getElementById('consent-birth');
  const purposeEl     = document.getElementById('consent-purpose');
  const agreeEl       = document.getElementById('consent-agree');
  const btnConfirm    = document.getElementById('consent-confirm');
  const btnCancel     = document.getElementById('consent-cancel');
  const btnClose      = document.getElementById('consent-close');

  function openConsentModal() {
    if (!consentModal) return;
    consentForm?.reset();                  // 매번 새로 입력
    btnConfirm?.setAttribute('disabled','');
    consentModal.style.display = 'block';
    document.body.style.overflow = 'hidden';
  }
  function closeConsentModal() {
    if (!consentModal) return;
    consentModal.style.display = 'none';
    document.body.style.overflow = '';
  }
  function validateConsent() {
    const ok = !!nameEl?.value.trim() && !!birthEl?.value && !!purposeEl?.value.trim() && !!agreeEl?.checked;
    if (ok) btnConfirm?.removeAttribute('disabled'); else btnConfirm?.setAttribute('disabled','');
  }
  [nameEl, birthEl, purposeEl].forEach(el => el?.addEventListener('input', validateConsent));
  agreeEl?.addEventListener('change', validateConsent);
  btnClose   ?.addEventListener('click', closeConsentModal);
  btnCancel  ?.addEventListener('click', closeConsentModal);
  document.querySelector('#consent-modal .ai-modal__backdrop')?.addEventListener('click', closeConsentModal);
  // 확인 → 동의값 검증 후 분석 시작(저장 안 함)
  btnConfirm?.addEventListener('click', (e) => {
    e.preventDefault();
    validateConsent();
    if (btnConfirm?.hasAttribute('disabled')) return;
    closeConsentModal();
    startReportAnalysis();
  });
  // "리포트 분석" 버튼을 누를 때는 무조건 모달을 띄움
  generateBtn?.addEventListener('click', (e) => {
    e.preventDefault();
    if (generateBtn.disabled) return;
    openConsentModal();
  });
  // (선택) 저장값 미사용 → 안전하게 null 반환하는 스텁
  function getStoredConsent(){ return null; }
  (function prefillConsentForm() {
    const c = getStoredConsent(); if (!c) return;
    if (nameEl && !nameEl.value) nameEl.value = c.name || '';
    if (birthEl && !birthEl.value) birthEl.value = c.birth || '';
    if (purposeEl && !purposeEl.value) purposeEl.value = c.purpose || '';
    if (agreeEl) agreeEl.checked = false;
  })();

  /* ================== 4) 인쇄 / 엑셀 / 새 창 ================== */
  function bindReportActions(){
    const once = (el, ev, fn) => { if(!el) return; if(el._b){ el.removeEventListener(ev, el._b); } el._b=fn; el.addEventListener(ev, fn); };

    const printBtn = document.getElementById('ai-modal-print');
    const excelBtn = document.getElementById('ai-modal-excel');
    const popBtn   = document.getElementById('ai-modal-popout');

    // 인쇄(PDF)
    once(printBtn, 'click', () => {
      if (!lastReportHtml) { alert('먼저 리포트를 생성하세요.'); return; }
      const html = `<html><head><meta charset="utf-8"><title>AI 리포트</title>
        <style>body{font-family:Arial,Helvetica,sans-serif;margin:24px}</style></head>
        <body>${lastReportHtml}<script>window.onload=()=>setTimeout(()=>print(),200)</script></body></html>`;
      const w = window.open('about:blank','_blank'); w.document.write(html); w.document.close();
    });

    // 엑셀 (ExcelJS 없으면 CSV)
    once(excelBtn, 'click', async () => {
      if (!lastStatsJson) { alert('먼저 리포트를 생성하세요.'); return; }
      if (window.ExcelJS) {
        const wb = new ExcelJS.Workbook(); wb.created = new Date();

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
        const mdToLines = (md='') => md
          .replace(/```[\s\S]*?```/g,'').replace(/^>+\s?/gm,'').replace(/!\[[^\]]*]\([^)]+\)/g,'')
          .replace(/\[([^\]]+)]\([^)]+\)/g,'$1').replace(/\*\*(.*?)\*\*/g,'$1')
          .replace(/__(.*?)__/g,'$1').replace(/\*(.*?)\*/g,'$1').replace(/_(.*?)_/g,'$1')
          .replace(/`([^`]+)`/g,'$1').replace(/^#{1,6}\s*/gm,'').split('\n').map(t=>t.trim()).filter(Boolean);

        secs.forEach(sec => {
          wsSum.mergeCells(r,2,r,3);
          const th = wsSum.getCell(r,2);
          th.value = (sec.title || '').replace(/^#{1,6}\s*/, '');
          th.font = { bold:true, size:14, color:{argb:'FF1E3A8A'} };
          th.fill = { type:'pattern', pattern:'solid', fgColor:{argb:'FFEFF6FF'} };
          r++;
          mdToLines(sec.content || '').forEach(line => {
            wsSum.mergeCells(r,2,r,3);
            const c = wsSum.getCell(r,2);
            c.value = line; c.alignment = { wrapText:true }; r++;
          });
          r += 1;
        });

        const ws = wb.addWorksheet('데이터', { views:[{ showGridLines:false, state:'frozen', ySplit:1 }] });
        ws.columns = [{width:24},{width:18},{width:10}];

        const addTitle = (row, text) => {
          ws.mergeCells(row,1,row,3);
          const c = ws.getCell(row,1);
          c.value = text; c.font = { bold:true, size:13, color:{argb:'FF1E3A8A'} };
          c.fill = { type:'pattern', pattern:'solid', fgColor:{argb:'FFEFF6FF'} };
          ws.getRow(row).height = 20; return row + 1;
        };
        const toPairs = obj => (obj?.labels && obj?.data) ? obj.labels.map((lab,i)=> [lab, Number(obj.data[i])]) : [];
        let cur = 2;

        const addTable = (name, rows, unit, numFmt) => {
          if (!rows || !rows.length) return;
          cur = addTitle(cur, `${name} (단위: ${unit})`);
          const ref = `A${cur}`;
          ws.addTable({
            name: `T_${name.replace(/\W+/g,'_')}_${cur}`,
            ref, headerRow: true, style: { theme: 'TableStyleMedium9', showRowStripes: true },
            columns: [{name:'라벨'},{name:'값'},{name:'단위'}],
            rows: rows.map(([lab, val]) => [lab, (val ?? ''), unit])
          });
          const start = cur + 1, end = cur + rows.length;
          for (let rr = start; rr <= end; rr++) ws.getCell(rr, 2).numFmt = numFmt;
          cur = end + 3;
        };

        addTable('가입자-일별',   toPairs(lastStatsJson.subscribers?.daily),     '명', '#,##0');
        addTable('가입자-월별',   toPairs(lastStatsJson.subscribers?.monthly),   '명', '#,##0');
        addTable('가입자-분기별', toPairs(lastStatsJson.subscribers?.quarterly), '명', '#,##0');
        if (lastStatsJson.demographics?.age)
          addTable('연령대', Object.entries(lastStatsJson.demographics.age).map(([k,v])=>[k,Number(v)]), '명', '#,##0');
        if (lastStatsJson.demographics?.gender)
          addTable('성별', Object.entries(lastStatsJson.demographics.gender).map(([k,v])=>[k,Number(v)]), '명', '#,##0');
        addTable('리뷰-월별갯수', toPairs(lastStatsJson.reviews?.countMonthly),   '건', '#,##0');
        addTable('평점-월별평균', toPairs(lastStatsJson.reviews?.ratingMonthly), '점', '0.0');
        addTable('누적 원화 사용액', toPairs(lastStatsJson.usage?.krwCumulative), '원', '#,##0');

        // 관심 통화/분야
        if (lastStatsJson.affinity?.currency) {
          const rows = Object.entries(lastStatsJson.affinity.currency).map(([k,v]) => [k, Number(v)]);
          addTable('관심 통화 분포', rows, '명', '#,##0');
        }
        if (lastStatsJson.affinity?.topic?.labels && lastStatsJson.affinity?.topic?.data) {
          const rows = lastStatsJson.affinity.topic.labels.map((lab, i) => [lab, Number(lastStatsJson.affinity.topic.data[i])]);
          addTable('관심 분야 분포', rows, '명', '#,##0');
        }

        const buf = await wb.xlsx.writeBuffer();
        const blob = new Blob([buf], { type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob); a.download = `AI_리포트_표_${new Date().toISOString().slice(0,10)}.xlsx`;
        a.click(); setTimeout(()=> URL.revokeObjectURL(a.href), 1000);
        return;
      }

      // ExcelJS 없으면 CSV
      const rows = []; rows.push(['섹션','라벨','값','단위']);
      const pushSeries = (section, obj, unit) => { if (!obj?.labels || !obj?.data) return; obj.labels.forEach((lab,i)=> rows.push([section, lab, obj.data[i], unit])); };
      pushSeries('가입자-일별',   lastStatsJson.subscribers?.daily,     '명');
      pushSeries('가입자-월별',   lastStatsJson.subscribers?.monthly,   '명');
      pushSeries('가입자-분기별', lastStatsJson.subscribers?.quarterly, '명');
      if (lastStatsJson.demographics?.age) Object.entries(lastStatsJson.demographics.age).forEach(([k,v])=> rows.push(['연령대', k, v, '명']));
      if (lastStatsJson.demographics?.gender) Object.entries(lastStatsJson.demographics.gender).forEach(([k,v])=> rows.push(['성별', k, v, '명']));
      pushSeries('리뷰-월별갯수', lastStatsJson.reviews?.countMonthly, '건');
      pushSeries('평점-월별평균', lastStatsJson.reviews?.ratingMonthly, '점');
      pushSeries('누적원화',      lastStatsJson.usage?.krwCumulative,  '원');
      if (lastStatsJson.affinity?.currency)
        Object.entries(lastStatsJson.affinity.currency).forEach(([k,v]) => rows.push(['관심 통화 분포', k, v, '명']));
      if (lastStatsJson.affinity?.topic?.labels && lastStatsJson.affinity?.topic?.data)
        lastStatsJson.affinity.topic.labels.forEach((lab,i)=> rows.push(['관심 분야 분포', lab, lastStatsJson.affinity.topic.data[i], '명']));

      const csv = '\uFEFF' + rows.map(r => r.map(v => `"${(v ?? '').toString().replace(/"/g,'""')}"`).join(',')).join('\n');
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href = url; a.download = `dashboard-report_${new Date().toISOString().slice(0,10)}.csv`;
      a.click(); setTimeout(()=> URL.revokeObjectURL(url), 1000);
    });

    // 새 창
    once(popBtn, 'click', () => {
      if (!lastReportHtml) { alert('먼저 리포트를 생성하세요.'); return; }
      const w = window.open('about:blank','_blank');
      w.document.write(`<html><head><meta charset="utf-8"><title>AI 리포트</title></head><body style="font-family:Arial,Helvetica,sans-serif;margin:24px">${lastReportHtml}</body></html>`);
      w.document.close();
    });
  } // bindReportActions

}); // ← DOMContentLoaded 종료 (필수!)
