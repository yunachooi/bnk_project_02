// static/js/echarts-dashboard.js
document.addEventListener('DOMContentLoaded', () => {
	
	/* ───────────── 공통 날짜 유틸 ────────────────────────────── */
  const today = new Date();
  
  // 최근 N 일(←오름차순) -- 'M/D' 형식
  const lastNDays = (n) =>
    Array.from({ length: n }, (_, i) => {
      const d = new Date(today);
      d.setDate(today.getDate() - (n - 1 - i));
      return `${d.getMonth() + 1}/${d.getDate()}`;
    });
	
	// 최근 N 분기(←오름차순)  -- 'YYYY Qn' 형식
	const lastNQuarters = (n) =>
	  Array.from({ length: n }, (_, i) => {
	    const d = new Date(today);
	    d.setMonth(today.getMonth() - 3 * (n - 1 - i));
	    const q = Math.floor(d.getMonth() / 3) + 1;
	    return `${d.getFullYear()} Q${q}`;
	  }); 
	
	// 최근 N 개월(←오름차순) -- 'M월' 형식
	const lastNMonths = (n) =>
	  Array.from({ length: n }, (_, i) => {
	    const d = new Date(today);
	    d.setMonth(today.getMonth() - (n - 1 - i));
	    return `${d.getMonth() + 1}월`;
	  });
	  
	  /* ───────────── 1) 가입자수 라인 (일/월/분기) ───────────── */
	  const dailyLabels     = lastNDays(7);
	  const monthlyLabels   = lastNMonths(6);
	  const quarterlyLabels = lastNQuarters(6);	    


  /* ------------------------------------------------------------------ */
  /* 1) 가입자수 라인 (일/월/분기)                                       */
  /* ------------------------------------------------------------------ */

  const dailyChart = echarts.init(document.getElementById('chart-daily'));
    dailyChart.setOption({
      xAxis: { type: 'category', data: dailyLabels },
      yAxis: { type: 'value' },
	  tooltip:{                     // ← 추가
	        trigger:'axis',
	        formatter: p =>             // p = params 배열
	          `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`
	      },
      series: [{ data: [5, 9, 6, 7, 10], type: 'line', smooth: true }]
    });

    // 월별 가입자 수
    const monthlyChart = echarts.init(document.getElementById('chart-monthly'));
    monthlyChart.setOption({
      xAxis: { type: 'category', data: monthlyLabels },
      yAxis: { type: 'value' },
	  tooltip:{                     // ← 추가
	        trigger:'axis',
	        formatter: p =>             // p = params 배열
	          `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`
	      },
      series: [{ data: [30, 50, 60, 80, 120], type: 'line', smooth: true }]
    });

    // 분기별 가입자 수
    const quarterlyChart = echarts.init(document.getElementById('chart-quarterly'));
    quarterlyChart.setOption({
      xAxis: { type: 'category', data: quarterlyLabels },
      yAxis: { type: 'value' },
	  tooltip:{                     // ← 추가
	        trigger:'axis',
	        formatter: p =>             // p = params 배열
	          `${p[0].axisValue}<br/>가입자수: <b>${p[0].data}</b>명`
	      },
      series: [{ data: [100, 180, 240], type: 'line', smooth: true }]
    });
  /* ------------------------------------------------------------------ */
  /* 2) 연령대 bar / 성별 pie                                            */
  /* ------------------------------------------------------------------ */

  fetch('/api/ageStats')
    .then(res => res.json())
    .then(data => {
      const labels = Object.keys(data);
      const counts = Object.values(data);

      echarts.init(document.getElementById('age-bar')).setOption({
        xAxis: { type: 'category', data: labels },
        yAxis: { type: 'value' },
        tooltip: { trigger: 'axis' },
        series: [{
          name: '가입자',
          type: 'bar',
          itemStyle: { color: '#4e81ff' },
          data: counts
        }]
      });
    });

  echarts.init(document.getElementById('gender-pie')).setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: '60%',
      data: [
        { value: 1600, name:'남성' },
        { value: 1400, name:'여성' }
      ]
    }]
  });

  /* ------------------------------------------------------------------ */
  /* 3) 리뷰 지표들                                                      */
  /* ------------------------------------------------------------------ */

  echarts.init(document.getElementById('review-count-bar')).setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category',
            data: monthlyLabels },
    yAxis:{ type:'value' },
    series:[{ type:'bar', data:[18,32,26,44,38,51],
              itemStyle:{ color:'#ffa940' }}]
  });

  echarts.init(document.getElementById('rating-line')).setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category',
            data: monthlyLabels},
    yAxis:{ type:'value', max:5, min:0 },
    series:[{ type:'line', data:[4.1,4.2,4.0,4.3,4.4,4.5],
              smooth:true, areaStyle:{} }]
  });

  // wordcloud 플러그인을 쓰려면 echarts-wordcloud.js를 추가해야 함
  // 예시: 긍·부정 키워드 (더미)
  const wcOption = {
    tooltip:{},
    series:[{
      type:'wordCloud',
      gridSize:2,
      shape:'circle',
      sizeRange:[10,40],
      textStyle:{ color: () => '#' +
        Math.floor(Math.random()*0xffffff).toString(16).padStart(6,'0') },
      data:[
        { name:'환율우대', value:60 },
        { name:'간편결제', value:45 },
        { name:'수수료높음', value:30 },
        { name:'속도빠름', value:35 },
        { name:'UI불편', value:20 },
        { name:'친절', value:25 },
        { name:'오류', value:18 }
      ]
    }]
  };
  if (echarts.wordcloud) {
    echarts.init(document.getElementById('sentiment-wordcloud')).setOption(wcOption);
  } else {
    document.getElementById('sentiment-wordcloud')
            .innerText = 'wordCloud 플러그인을 로드하세요.';
  }

  /* ------------------------------------------------------------------ */
  /* 4) 누적 원화 & 공유 클릭 수                                         */
  /* ------------------------------------------------------------------ */

  echarts.init(document.getElementById('krw-area')).setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category',
            data: quarterlyLabels},
    yAxis:{ type:'value',
            axisLabel: v => (v/1e8)+'억' },
    series:[{ type:'line', areaStyle:{},
              data:[5e8,8e8,1.3e9,1.9e9,2.5e9,3.2e9] }]
  });

  echarts.init(document.getElementById('share-gauge')).setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyLabels },
    yAxis: { type: 'value' },
    series: [{
      name: '클릭 수',
      type: 'line',
      smooth: true,
      areaStyle: {},
      data: [502, 640, 735, 812, 955, 1023, 982]  // 더미 데이터
    }]
  });

  /* ------------------------------------------------------------------ */
  /* 5) 최근 리뷰 더미 리스트                                            */
  /* ------------------------------------------------------------------ */

  const reviews = [
    { date:'2025-08-01', nick:'kfx***', rate:4.5,
      text:'환전 수수료가 낮아져서 만족합니다.' },
    { date:'2025-07-29', nick:'blue***', rate:3.5,
      text:'앱이 가끔 끊겨요. 개선되면 좋겠습니다.' },
    { date:'2025-07-20', nick:'hana***', rate:5.0,
      text:'해외 송금 속도가 빨라서 편해요!' }
  ];
  const tbody = document.getElementById('review-list');
  reviews.forEach(r => {
    tbody.insertAdjacentHTML('beforeend', `
      <tr>
        <td>${r.date}</td>
        <td>${r.nick}</td>
        <td>${r.rate}</td>
        <td>${r.text}</td>
      </tr>`);
  });

});


// 2) 차트에서 데이터 뽑는 유틸
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

// 3) 화면에 있는 모든 지표를 하나의 JSON으로 수집
async function buildStatsJson() {
  // 가입자 추이
  const subscribers = {
    daily: extractSeriesDataByDomId('chart-daily'),
    monthly: extractSeriesDataByDomId('chart-monthly'),
    quarterly: extractSeriesDataByDomId('chart-quarterly')
  };

  // 연령대: 서버 API 다시 호출(타이밍 보장용)
  let ageStats = null;
  try {
    const res = await fetch('/api/ageStats');
    if (res.ok) ageStats = await res.json();
  } catch (_) {}

  // 성별: 파이 차트에서 추출
  let gender = null;
  const gInst = echarts.getInstanceByDom(document.getElementById('gender-pie'));
  if (gInst) {
    const gopt = gInst.getOption();
    const arr = (gopt.series && gopt.series[0] && gopt.series[0].data) ? gopt.series[0].data : [];
    gender = arr.reduce((acc, cur) => (acc[cur.name] = cur.value, acc), {});
  }

  // 리뷰 지표
  const reviewsCountMonthly = extractSeriesDataByDomId('review-count-bar');
  const ratingMonthly      = extractSeriesDataByDomId('rating-line');

  // 워드클라우드(있으면)
  let keywords = null;
  const wcEl = document.getElementById('sentiment-wordcloud');
  if (wcEl) {
    const wcInst = echarts.getInstanceByDom(wcEl);
    if (wcInst) {
      const wopt = wcInst.getOption();
      keywords = (wopt.series && wopt.series[0] && wopt.series[0].data) ? wopt.series[0].data : null;
    }
  }

  // 사용액/공유 클릭
  const krwCumulative    = extractSeriesDataByDomId('krw-area');
  const shareClicksDaily = extractSeriesDataByDomId('share-gauge');

  // 최근 리뷰(테이블에서 추출)
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

// 4) 버튼 클릭 → 백엔드(OpenAI) 호출 → 결과 표시
const btn = document.getElementById('btn-generate-report');
const out = document.getElementById('ai-report');

btn?.addEventListener('click', async () => {
  try {
    out.textContent = 'AI 리포트를 생성 중입니다…';
    const stats = await buildStatsJson();

    const res = await fetch('/api/generateReport', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(stats)
    });

    const text = await res.text();
    out.textContent = text || '비어 있는 응답입니다.';
  } catch (e) {
    out.textContent = '리포트 생성 실패: ' + (e?.message || e);
  }
  
});

/* ================== 리포트 PDF/Excel 내보내기 ================== */

// ======== 공용 상태/참조 ========
const btn2      = document.getElementById('btn-generate-report');
const outEl     = document.getElementById('ai-report'); // 없을 수 있으니 가드로 사용
const pdfBtn    = document.getElementById('btn-open-pdf');
const excelBtn  = document.getElementById('btn-open-excel');

let lastReportText = '';
let lastStatsJson  = null;

function setOut(msg){
  if (outEl) outEl.textContent = msg;
  console.log(msg);
}

// ======== 단일 클릭 핸들러 ========
btn?.addEventListener('click', async () => {
	  try {
	    setOut('AI 리포트를 생성 중입니다…');

	    const stats = await buildStatsJson();
	    lastStatsJson = stats;

	    const res = await fetch('/api/generateReport', {
	      method: 'POST',
	      headers: { 'Content-Type': 'application/json' },
	      body: JSON.stringify(stats)
	    });

	    lastReportText = await res.text() || '비어 있는 응답입니다.';

	    // 기존 setOut 대신 모달로 표시
	    openReportModal(lastReportText);

	    // 내보내기 버튼 활성화
	    if (pdfBtn)   pdfBtn.disabled   = false;
	    if (excelBtn) excelBtn.disabled = false;

	  } catch (e) {
	    openReportModal('리포트 생성 실패: ' + (e?.message || e));
	  }
	});
// ======== PDF 보기 ========
pdfBtn?.addEventListener('click', () => {
  if (!lastReportText) { alert('먼저 리포트를 생성하세요.'); return; }
  const html = `
    <html><head><meta charset="utf-8"><title>AI 리포트</title>
    <style>body{font-family:Arial,Helvetica,sans-serif;margin:24px;line-height:1.6}
    h1{font-size:20px;margin-bottom:16px}pre{white-space:pre-wrap}.meta{color:#666;margin-bottom:16px}</style>
    </head><body>
    <h1>외환 대시보드 AI 리포트</h1>
    <div class="meta">생성시각: ${new Date().toLocaleString()}</div>
    <pre>${lastReportText.replace(/</g,'&lt;')}</pre>
    <script>window.onload=()=>setTimeout(()=>window.print(),300)</script>
    </body></html>`;
  const w = window.open('about:blank', '_blank');
  w.document.write(html); w.document.close();
});

// ======== Excel(CSV) 다운로드 ========
excelBtn?.addEventListener('click', () => {
  if (!lastStatsJson) { alert('먼저 리포트를 생성하세요.'); return; }

  const rows = [];
  rows.push(['섹션','라벨','값']);
  const pushSeries = (section, obj)=>{
    if (!obj) return;
    if (obj.labels && obj.data) obj.labels.forEach((lab,i)=> rows.push([section, lab, obj.data[i]]));
  };
  pushSeries('가입자-일별',      lastStatsJson.subscribers?.daily);
  pushSeries('가입자-월별',      lastStatsJson.subscribers?.monthly);
  pushSeries('가입자-분기별',    lastStatsJson.subscribers?.quarterly);
  if (lastStatsJson.demographics?.age)
    Object.entries(lastStatsJson.demographics.age).forEach(([k,v])=> rows.push(['연령대',k,v]));
  if (lastStatsJson.demographics?.gender)
    Object.entries(lastStatsJson.demographics.gender).forEach(([k,v])=> rows.push(['성별',k,v]));
  pushSeries('리뷰-월별갯수',    lastStatsJson.reviews?.countMonthly);
  pushSeries('평점-월별평균',    lastStatsJson.reviews?.ratingMonthly);
  pushSeries('누적원화',         lastStatsJson.usage?.krwCumulative);
  pushSeries('공유클릭-일별',    lastStatsJson.usage?.shareClicksDaily);

  const csv = '\uFEFF' + rows.map(r => r.map(v => `"${(v ?? '').toString().replace(/"/g,'""')}"`).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = `dashboard-report_${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
  setTimeout(()=> URL.revokeObjectURL(url), 1000);
  });