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
