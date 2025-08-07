// static/js/echarts-dashboard.js
document.addEventListener('DOMContentLoaded', () => {

  /* ------------------------------------------------------------------ */
  /* 1) 가입자수 라인 (일/월/분기)                                       */
  /* ------------------------------------------------------------------ */

  const subChart = echarts.init(document.getElementById('subscribers-line'));
  subChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['일', '월', '분기'] },
    xAxis: { type: 'category',
             data: ['1월','2월','3월','4월','5월','6월','7월','8월'] },
    yAxis: { type: 'value' },
    series: [
      { name: '일',  type: 'line', smooth: true,
        data: [12,14,18,11,13,16,14,17] },
      { name: '월',  type: 'line', areaStyle: {},
        data: [320,402,550,600,690,750,820,900] },
      { name: '분기',type: 'line',
        data: [900,1300,1500,1780,2050,2300,2500,2700] }
    ]
  });

  /* ------------------------------------------------------------------ */
  /* 2) 연령대 bar / 성별 pie                                            */
  /* ------------------------------------------------------------------ */

  echarts.init(document.getElementById('age-bar')).setOption({
    xAxis: { type: 'category',
             data: ['10대','20대','30대','40대','50대','60대+'] },
    yAxis: { type: 'value' },
    tooltip: { trigger: 'axis' },
    series: [{ name: '가입자', type: 'bar',
               itemStyle:{ color:'#4e81ff' },
               data: [120, 340, 280, 220, 140, 60] }]
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
            data:['3월','4월','5월','6월','7월','8월']},
    yAxis:{ type:'value' },
    series:[{ type:'bar', data:[18,32,26,44,38,51],
              itemStyle:{ color:'#ffa940' }}]
  });

  echarts.init(document.getElementById('rating-line')).setOption({
    tooltip:{ trigger:'axis' },
    xAxis:{ type:'category',
            data:['3월','4월','5월','6월','7월','8월']},
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
            data:['2023Q1','Q2','Q3','Q4','2024Q1','Q2']},
    yAxis:{ type:'value',
            axisLabel: v => (v/1e8)+'억' },
    series:[{ type:'line', areaStyle:{},
              data:[5e8,8e8,1.3e9,1.9e9,2.5e9,3.2e9] }]
  });

  echarts.init(document.getElementById('share-gauge')).setOption({
    series:[{
      type:'gauge',
      min:0, max:10000,
      detail:{ formatter:'{value}회' },
      data:[{ value: 7240, name:'클릭 수' }]
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
