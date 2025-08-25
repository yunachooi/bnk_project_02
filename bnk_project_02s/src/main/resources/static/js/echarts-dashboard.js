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
const formatKrw = n => (Number(n||0)).toLocaleString('ko-KR');
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
  (async function renderSubscribers(){
    const API_BASE = '/api/subscribers'; // 관리자 경로에 맞춤
    const chDaily     = getOrInitChart('chart-daily');
    const chMonthly   = getOrInitChart('chart-monthly');
    const chQuarterly = getOrInitChart('chart-quarterly');

    const render = (chart, payload) => chart?.setOption({
      xAxis:{ type:'category', data: payload.labels },
      yAxis:{ type:'value' },
      tooltip:{ trigger:'axis', formatter:p=>`${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명` },
      series:[{ type:'line', data: payload.data, smooth:true,
        areaStyle: blueArea(),
        lineStyle:{ width:3 },
        itemStyle:{ shadowBlur:6, shadowColor:'rgba(37,99,235,.25)' }
      }]
    });

    try {
      const d = await fetch(`${API_BASE}/daily?days=7`).then(r=>r.json());
      render(chDaily, d);
    } catch (e) { console.warn('가입자 일별 로드 실패', e); }

    try {
      const m = await fetch(`${API_BASE}/monthly?months=6`).then(r=>r.json());
      render(chMonthly, m);
    } catch (e) { console.warn('가입자 월별 로드 실패', e); }

    try {
      const q = await fetch(`${API_BASE}/quarterly?quarters=6`).then(r=>r.json());
      render(chQuarterly, q);
    } catch (e) { console.warn('가입자 분기별 로드 실패', e); }
  })();

  // 연령/성별
  fetch('/api/ageStats').then(r=>r.json()).then(data=>{
    getOrInitChart('age-bar')?.setOption({
      xAxis:{type:'category',data:Object.keys(data)}, yAxis:{type:'value'}, tooltip:{trigger:'axis'},
      series:[{type:'bar',data:Object.values(data),itemStyle:blueBar(),emphasis:{itemStyle:{shadowBlur:12,shadowColor:'rgba(37,99,235,.28)'}}}]
    });
  }).catch(()=>{});
  fetch('/api/genderStats')
    .then(r => r.json())
    .then(obj => {
      const pieData = [
        { name: '남성', value: Number(obj['남성'] ?? obj.male ?? 0) },
        { name: '여성', value: Number(obj['여성'] ?? obj.female ?? 0) }
      ];
      const other = Number(obj['기타'] ?? obj.other ?? 0);
      if (other > 0) pieData.push({ name: '기타', value: other });

      window.__genderPie = pieData;

      getOrInitChart('gender-pie')?.setOption({
        color:['#3b82f6','#93c5fd','#cbd5e1'],
        legend:{bottom:0},
        tooltip:{trigger:'item'},
        series:[{ type:'pie', radius:'60%', data: pieData }]
      });
    })
    .catch(() => {
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

  /* === 통화 코드/ISO → 한글 이름 매핑 === */
  const CURRENCY_NAME = {
    USD:'미국 달러', JPY:'일본 엔',
    CNH:'중국 위안(CNH)', CNY:'중국 위안', KRW:'한국 원화',
    EUR:'유로', CHF:'스위스 프랑', VND:'베트남 동', GBP:'영국 파운드',
    '840':'미국 달러(USD)', '392':'일본 엔(JPY)',
    '156':'중국 위안(CNY/CNH)', '978':'유로(EUR)',
    '756':'스위스 프랑(CHF)', '704':'베트남 동(VND)', '410': '원화(KRW)', '826': '영국 파운드(GBP)' 
  };
  const toCurName = k => CURRENCY_NAME[String(k).trim()] || String(k);

  // ====== 1) 총 원화 사용액 텍스트 ======
  (async function renderKrwTotal(){
    try {
      const res = await fetch('/api/usage/krw/total');
      const { totalKrw } = await res.json();
      const el = document.getElementById('krw-total');
      if (el) el.textContent = `총 원화 사용액: ${formatKrw(totalKrw)}원`;
    } catch(e) { console.warn('총 원화 사용액 로드 실패', e); }
  })();

  // ====== 2) 일자별 통화별 환전금액(외화) – 그룹 막대 ======
  (async function renderFxDaily(){
    try{
      const { labels, series } = await fetch('/api/usage/fx/daily?days=14').then(r=>r.json());
      const inst = getOrInitChart('fx-daily-stacked'); // 같은 DOM을 재사용
      if(!inst) return;

      inst.setOption({
        tooltip:{
          trigger:'axis',
          axisPointer:{ type:'shadow' },
          formatter: (items=[]) => {
            const date = items[0]?.axisValue || '';
            const lines = items.map(it => `${toCurName(it.seriesName)}: <b>${it.data}</b>`);
            return `${date}<br/>` + lines.join('<br/>');
          }
        },
        legend:{ top:0, type:'scroll', formatter: (name)=>toCurName(name) },
        grid:{left:40,right:20,top:36,bottom:28,containLabel:true},
        xAxis:{ type:'category', data:labels },
        yAxis:{ type:'value' },
        // ★ stack 제거 → 통화별 막대가 하루 기준으로 나란히 표시
        series:(series||[]).map((s,idx)=>({
          type:'bar',
          name: toCurName(s.name),
          data: s.data,
          // 보기 좋게 간격 조절
          barGap: '10%',
          barCategoryGap: '38%',
          // 테마 팔레트 색을 쓰도록 itemStyle은 최소화
          itemStyle:{ borderRadius:[8,8,0,0] },
          emphasis:{ itemStyle:{ shadowBlur:12, shadowColor:'rgba(37,99,235,.28)'} }
        }))
      });
    }catch(e){ console.warn('FX 일자별 합계 로드 실패', e); }
  })();

  // ====== 3) 일자별 환전금액(원화사용액) – 라인 ======
  (async function renderKrwDaily(){
    try{
      const { labels, data } = await fetch('/api/usage/krw/daily?days=14').then(r=>r.json());
      getOrInitChart('krw-daily-line')?.setOption({
        tooltip:{trigger:'axis', formatter:(p)=>`${p[0].axisValue}<br/>원화사용액: <b>${formatKrw(p[0].data)}</b>원`},
        xAxis:{type:'category',data:labels},
        yAxis:{type:'value', axisLabel:{formatter:(v)=>formatKrw(v)}},
        series:[{ type:'line', data, areaStyle: blueArea(), itemStyle:{shadowBlur:6,shadowColor:'rgba(37,99,235,.25)'}}]
      });
    }catch(e){ console.warn('KRW 일자별 합계 로드 실패', e); }
  })();

  // 관심 지표
  const CUR_ORDER   = ['USD','JPY','CNH','EUR','CHF','VND'];
  const TOPIC_ORDER = ['TRAVEL','STUDY','SHOPPING','FINANCE','ETC'];

  fetch('/api/interests/currencies')
    .then(r => r.json())
    .then(data => {
      const seriesData = CUR_ORDER.map(k => ({ name: k, value: Number(data?.[k] || 0) }));
      getOrInitChart('currency-pie')?.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 0, top: 'middle' },
        series: [{
          type: 'pie',
          radius: ['55%','78%'],
          center: ['40%','50%'],
          top: 8, bottom: 8,
          avoidLabelOverlap: true,
          minAngle: 6,
          label: { show: true, formatter: '{b}: {c}', fontSize: 12, overflow: 'truncate', ellipsis: '…' },
          labelLine: { length: 8, length2: 6 },
          data: seriesData
        }]
      });
    }).catch(()=>{});

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
    }).catch(()=>{});

  // ★ 워드클라우드
  (async function renderWordcloud(){
    const posChart = getOrInitChart('kw-pos');
    const negChart = getOrInitChart('kw-neg');
    if (!posChart && !negChart) return;
    try {
      const r = await fetch('/api/reviews/keywords?months=3');
      const { positive = [], negative = [] } = await r.json();

      const pickTop = (arr, n = 15) => [...arr].sort((a,b)=>(b.value||1)-(a.value||1)).slice(0, n);
      const toSeriesData = (arr, color) =>
        pickTop(arr, 15).filter(k => k && k.name && (k.value ?? 1) > 0)
                        .map(k => ({ name: k.name, value: k.value || 1, textStyle: { color } }));

      const base = (data, fontFamily) => ({
        type: 'wordCloud', left: '6%', right: '6%', top: '8%', bottom: '8%',
        gridSize: 20, sizeRange: [12, 24],
        rotationRange: [0, 0], layoutAnimation: false, shape: 'circle', drawOutOfBound: false,
        textStyle: { fontFamily }, emphasis: { focus: 'self', textStyle: { fontWeight: 900 } }, data
      });
      if (posChart) posChart.setOption({ tooltip: {}, series: [ base(toSeriesData(positive, '#2563eb'), 'SUIT Variable, Pretendard, system-ui, sans-serif') ] });
      if (negChart) negChart.setOption({ tooltip: {}, series: [ base(toSeriesData(negative, '#ef4444'), 'Pretendard, SUIT Variable, system-ui, sans-serif') ] });

      if ((!positive.length) && (!negative.length)) {
        const setEmpty = (inst) => { const dom = inst?.getDom?.(); if (!dom) return;
          dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#64748b">표시할 키워드가 없습니다.</div>'; };
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
            const rawDate = r.rvdate ?? r.rvDate ?? r.date ?? r.createdAt ?? '';
            const date = (()=>{  // 2025-08-21 10:23:33 -> 2025-08-21
              if(!rawDate) return '';
              const m = String(rawDate).match(/^(\d{4}-\d{2}-\d{2})(?:[ T](\d{2}:\d{2}))?/);
              return m ? m[1] : String(rawDate).slice(0,10);
            })();
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
    'currency-pie','interest-bar',
    'kw-pos','kw-neg',
    'fx-daily-stacked','krw-daily-line'
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

    let labels = [];
    if (opt.xAxis?.[0]?.data?.length) labels = opt.xAxis[0].data;
    else if (opt.yAxis?.[0]?.data?.length) labels = opt.yAxis[0].data;
    else if (rawData.length && typeof rawData[0] === 'object' && 'name' in rawData[0]) {
      labels = rawData.map(d => d.name);
    }
    const data = rawData.map(v => (typeof v === 'object' && v !== null && 'value' in v) ? v.value : v);
    return { labels, data };
  }
  function extractPieAsObject(domId){
    const el = document.getElementById(domId);
    const inst = el && echarts.getInstanceByDom(el);
    if (!inst) return null;
    const data = inst.getOption()?.series?.[0]?.data || [];
    const out = {}; data.forEach(d => out[d.name] = d.value); return out;
  }
  function extractMultiSeriesByDomId(domId){
    const el = document.getElementById(domId);
    const inst = el && echarts.getInstanceByDom(el);
    if (!inst) return null;
    const opt = inst.getOption() || {};
    const labels = (opt.xAxis?.[0]?.data) || (opt.yAxis?.[0]?.data) || [];
    const series = (opt.series || []).map(s => ({
      name: s.name || '',
      data: (s.data || []).map(v => (typeof v === 'object' && v !== null && 'value' in v) ? v.value : v)
    }));
    return { labels, series };
  }

  async function buildStatsJson() {
    // 리뷰 시계열 확보(미초기화 대비 서버에서 재조회)
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

    // 가입자
    const subscribers = {
      daily:     extractSeriesDataByDomId('chart-daily'),
      monthly:   extractSeriesDataByDomId('chart-monthly'),
      quarterly: extractSeriesDataByDomId('chart-quarterly')
    };

    // 인구통계
    let ageStats = null;
    try { const res = await fetch('/api/ageStats'); if (res.ok) ageStats = await res.json(); } catch (_) {}
    let gender = null;
    const gInst = echarts.getInstanceByDom(document.getElementById('gender-pie'));
    if (gInst) {
      const arr = (gInst.getOption().series?.[0]?.data) || [];
      gender = arr.reduce((acc, cur) => (acc[cur.name] = cur.value, acc), {});
    }

    // 관심
    const currencyDist = extractPieAsObject('currency-pie');
    const topicSeries  = extractSeriesDataByDomId('interest-bar');
    const topicMap = (() => {
      const m = {};
      if (topicSeries?.labels?.length) {
        topicSeries.labels.forEach((lab, i) => m[lab] = Number(topicSeries.data?.[i] || 0));
      }
      return m;
    })();

    // 최근 리뷰(표 렌더링 데이터 그대로 긁음)
    const recentReviews = Array.from(document.querySelectorAll('#review-list tr')).map(tr => {
      const tds = tr.querySelectorAll('td');
      return { date: tds[0]?.textContent?.trim(), nick: tds[1]?.textContent?.trim(),
               rate: tds[2]?.textContent?.trim(), text: tds[3]?.textContent?.trim() };
    });

    // 신규 사용액 지표
    const krwTotal = (() => {
      const el = document.getElementById('krw-total');
      if (!el) return null;
      const m = el.textContent.match(/([\d,]+)/);
      return m ? Number(m[1].replace(/,/g, '')) : null;
    })();
    const krwDaily = extractSeriesDataByDomId('krw-daily-line');   // {labels,data}
    const fxDaily  = extractMultiSeriesByDomId('fx-daily-stacked');// {labels,series:[{name,data}]}

    return {
      generatedAt: new Date().toISOString(),
      subscribers,
      demographics: { age: ageStats, gender },
      reviews: { countMonthly: reviewsCountMonthly, ratingMonthly, recentReviews, keywords: null },
      usage: {
        krwTotal,               // 총 원화 누적사용액(숫자)
        krwDaily,               // 일자별 환전금액(원화사용액)
        fxDaily                 // 일자별 통화별 환전금액(외화)
      },
      affinity: {
        currency: currencyDist,
        topic: { labels: topicSeries?.labels || [], data: topicSeries?.data || [] },
        topicMap
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
    'currency-pie':     { label:'관심 통화 분포' },
    'interest-bar':     { label:'관심 분야 분포' },
    'fx-daily-stacked': { label:'일자별 통화별 환전금액(외화)' },
    'krw-daily-line':   { label:'일자별 환전금액(원화사용액)' }
  };
  function getChartIdsForTitle(title='') {
    const t = title.toLowerCase();
    const ids = new Set();
    if (t.includes('가입자')) { ids.add('chart-daily'); ids.add('chart-monthly'); ids.add('chart-quarterly'); }
    if (t.includes('연령') || t.includes('성별')) { ids.add('age-bar'); ids.add('gender-pie'); }
    if (t.includes('리뷰') || t.includes('평점')) { ids.add('review-count-bar'); ids.add('rating-line'); }
    if (t.includes('환전') || t.includes('사용액') || t.includes('원화')) {
      ids.add('fx-daily-stacked'); ids.add('krw-daily-line');
    }
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
    bindReportActions();
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

  /* ================== 동의 모달 ================== */
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
    consentForm?.reset();
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

  btnConfirm?.addEventListener('click', (e) => {
    e.preventDefault();
    validateConsent();
    if (btnConfirm?.hasAttribute('disabled')) return;
    closeConsentModal();
    startReportAnalysis();
  });
  generateBtn?.addEventListener('click', (e) => {
    e.preventDefault();
    if (generateBtn.disabled) return;
    openConsentModal();
  });
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

        // 신규 지표
        addTable('일자별 환전금액(원화사용액)', toPairs(lastStatsJson.usage?.krwDaily), '원', '#,##0');

        // fxDaily: 날짜×통화 테이블로 평탄화
        if (lastStatsJson.usage?.fxDaily?.labels?.length && Array.isArray(lastStatsJson.usage.fxDaily.series)) {
          const rowsFx = [];
          const labels = lastStatsJson.usage.fxDaily.labels;
          lastStatsJson.usage.fxDaily.series.forEach(s => {
            labels.forEach((lab, i) => rowsFx.push([lab, s.name, Number(s.data?.[i] || 0)]));
          });
          cur = addTitle(cur, '일자별 통화별 환전금액(외화)');
          const refFx = `A${cur}`;
          ws.addTable({
            name:`T_FX_${cur}`, ref: refFx, headerRow:true,
            style:{ theme:'TableStyleMedium3', showRowStripes:true },
            columns:[{name:'날짜'},{name:'통화'},{name:'금액'}],
            rows: rowsFx
          });
          const start = cur + 1, end = cur + rowsFx.length;
          for (let rr=start; rr<=end; rr++) ws.getCell(rr,3).numFmt = '#,##0.####';
          cur = end + 3;
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
      if (lastStatsJson.demographics?.age)     Object.entries(lastStatsJson.demographics.age).forEach(([k,v])=> rows.push(['연령대', k, v, '명']));
      if (lastStatsJson.demographics?.gender)  Object.entries(lastStatsJson.demographics.gender).forEach(([k,v])=> rows.push(['성별', k, v, '명']));
      pushSeries('리뷰-월별갯수', lastStatsJson.reviews?.countMonthly,  '건');
      pushSeries('평점-월별평균', lastStatsJson.reviews?.ratingMonthly, '점');

      // 신규 지표 CSV
      pushSeries('일자별 환전금액(원화사용액)', lastStatsJson.usage?.krwDaily, '원');
      if (lastStatsJson.usage?.fxDaily?.labels?.length && Array.isArray(lastStatsJson.usage.fxDaily.series)) {
        const labels = lastStatsJson.usage.fxDaily.labels;
        lastStatsJson.usage.fxDaily.series.forEach(s => {
          labels.forEach((lab, i) => rows.push(['일자별 통화별 환전금액(외화)', `${lab} / ${s.name}`, s.data?.[i] || 0, '통화단위']));
        });
      }

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
