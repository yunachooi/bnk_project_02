document.addEventListener('DOMContentLoaded', () => {
  // 칩 활성화 스타일(라벨 클릭 시)
  document.querySelectorAll('.chip input').forEach(inp => {
    const sync = () => { /* CSS가 라디오/체크드 상태를 처리하므로 별도 로직 불필요 */ };
    inp.addEventListener('change', sync);
    sync();
  });

  // 관심통화 최대 N개 제한 (data-max)
  document.querySelectorAll('.chip-grid[data-max]').forEach(grid => {
    const max = parseInt(grid.dataset.max || '999', 10);
    const inputs = grid.querySelectorAll('input[type="checkbox"]');
    const enforce = () => {
      const checked = [...inputs].filter(i => i.checked);
      const over = checked.length > max;
      if (over) {
        // 가장 최근 클릭(이벤트 발생한 것)이 무엇인지 분간 어렵다면 마지막 체크된 것을 해제
        checked[checked.length-1].checked = false;
      }
    };
    inputs.forEach(i => i.addEventListener('change', enforce));
  });
});
