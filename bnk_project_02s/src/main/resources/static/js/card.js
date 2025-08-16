let cardInfo = null;
let showingFullNumber = false;
const baseUrl = window.location.origin;

window.addEventListener('load', function() {
    loadCardInfo();
});

async function loadCardInfo() {
    try {
        const response = await fetch(`${baseUrl}/user/card/info`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            cardInfo = await response.json();
            updateUI();
            showCardContent();
        } else if (response.status === 404) {
            showNoCardMessage();
        } else {
            showError('카드 정보를 불러오는데 실패했습니다.');
        }
    } catch (error) {
        showError('카드 정보를 불러오는 중 오류가 발생했습니다.');
    }

    hideLoading();
}

function showCardContent() {
    document.getElementById('card-content').style.display = 'block';
    document.getElementById('error-container').innerHTML = '';
    document.getElementById('no-card-container').innerHTML = '';
}

function showError(message) {
    document.getElementById('card-content').style.display = 'none';
    document.getElementById('error-container').innerHTML =
        `<div class="error-message">${message}</div>`;
}

function showNoCardMessage() {
    document.getElementById('card-content').style.display = 'none';
    document.getElementById('no-card-container').innerHTML =
        `<div class="no-card-message">
            <h3>등록된 카드가 없습니다</h3>
            <p>카드를 먼저 발급받아 주세요.</p>
        </div>`;
}

function updateUI() {
    if (!cardInfo || !cardInfo.cardno) return;

    const maskedCardNumber = maskCardNumber(cardInfo.cardno);
    const lastFour = getLastFourDigits(cardInfo.cardno);
    const visualCardNumber = maskCardNumberForVisual(cardInfo.cardno);

    document.getElementById('cardVisualNumber').textContent = visualCardNumber;
    document.getElementById('cardVisualName').textContent =
        cardInfo.cardname || 'BNK 쇼핑환전체크카드';

    document.getElementById('cardInfoName').textContent =
        cardInfo.cardname || 'BNK 쇼핑환전체크카드';
    document.getElementById('cardLastDigits').textContent = `(${lastFour})`;

    document.getElementById('cardNumberDisplay').textContent = maskedCardNumber;

    const switchElement = document.getElementById('cardStatusSwitch');
    if (cardInfo.cardstatus === 'Y') {
        switchElement.classList.add('active');
    } else {
        switchElement.classList.remove('active');
    }
}

function maskCardNumber(cardNumber) {
    if (!cardNumber || cardNumber.length < 16) return '4***-****-****-1234';

    const first1 = cardNumber.substring(0, 1);
    const last4 = cardNumber.substring(cardNumber.length - 4);

    return `${first1}***-****-****-${last4}`;
}

function maskCardNumberForVisual(cardNumber) {
    if (!cardNumber || cardNumber.length < 16) return '4*** **** **** 1234';

    const first1 = cardNumber.substring(0, 1);
    const last4 = cardNumber.substring(cardNumber.length - 4);

    return `${first1}*** **** **** ${last4}`;
}

function getLastFourDigits(cardNumber) {
    if (!cardNumber || cardNumber.length < 4) return '1234';
    return cardNumber.substring(cardNumber.length - 4);
}

async function toggleCardNumber() {
    const button = document.getElementById('toggleCardNumber');

    if (!showingFullNumber) {
        try {
            const response = await fetch(`${baseUrl}/user/card/full-number`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                const fullNumber = data.cardno;
                showingFullNumber = true;
                button.textContent = '카드번호 숨기기';
                document.getElementById('cardNumberDisplay').textContent = fullNumber;
            } else {
                showMessage('카드번호를 불러올 수 없습니다.');
            }
        } catch (error) {
            showMessage('카드번호 조회 중 오류가 발생했습니다.');
        }
    } else {
        showingFullNumber = false;
        button.textContent = '카드번호 보기';
        updateUI();
    }
}

async function toggleCardStatus() {
    try {
        const response = await fetch(`${baseUrl}/user/card/toggle-status`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            if (data.message) {
                cardInfo.cardstatus = cardInfo.cardstatus === 'Y' ? 'N' : 'Y';
                updateUI();
                showMessage(data.message);
            }
        } else {
            const errorData = await response.json();
            showMessage(errorData.error || '카드 상태 변경에 실패했습니다.');
        }
    } catch (error) {
        showMessage('카드 상태 변경 중 오류가 발생했습니다.');
    }
}


function hideLoading() {
    document.getElementById('loading').classList.add('hidden');
    document.getElementById('main-content').classList.remove('hidden');
}

function showMessage(message) {
    const snackbar = document.getElementById('snackbar');
    snackbar.textContent = message;
    snackbar.style.display = 'block';

    setTimeout(() => {
        snackbar.style.display = 'none';
    }, 3000);
}

function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = '/foreign0';
    }
}

function showCardBenefits() {
}

function reportLostCard() {
}

function cancelCard() {
}

function changePaymentDate() {
}