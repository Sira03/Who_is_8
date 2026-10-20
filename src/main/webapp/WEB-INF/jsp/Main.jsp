<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="ko">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Who Is... | 게이머 성향 분석 서비스</title>

    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
    <link rel="stylesheet" href="Style.css">
</head>

<body class="main">
    <header class="top">
        <div>
            <h1 class="top-text">Who Is...</h1>
        </div>
    </header>

    <section class="search-box">
        <div class="search-overlay"></div>
        <div class="search-content">
            <label class="search-label">소환사 이름 검색 (Riot ID)</label>
            <div class="search-area">
                <input
                    type="text"
                    id="summonerInput"
                    value=""
                    placeholder="소환자명#태그 (예: Hide on bush#KR1)"
                    class="search-input"
                >
                <button onclick="searchPersona()" class="search-button">성향 분석</button>
            </div>
        </div>
    </section>

    <main id="resultDashboard" class="dashboard hidden">
        <div id="mainNavigation" class="result-tabs">
            <button class="result-tab active" onclick="showTab(0)">종합</button>
            <button class="result-tab" onclick="showTab(1)">최근 전적</button>
        </div>

        <!-- 종합 -->
        <div id="tabContent0" class="tab-content active">
            <div class="card">
                <div class="result">
                    <div>
                        <span class="analysis">PLAYER INFO</span>
                        <div class="user-profile">
                            <img id="userIcon" class="user-icon" src="" alt="유저 아이콘">
                            <div class="user-info">
                                <h2 id="userName" class="name">유저명</h2>
                                <div class="user-tier" id="userTierArea">
                                    <img id="tierIcon" class="tier-icon" src="" alt="" style="display:none;">
                                    <span id="userRank" class="rank-text">티어 정보 없음</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="answer">
                        <span class="game" id="matchCountLabel">최근 10게임 기준</span>
                        <p class="rate" id="kdaValue">KDA -</p>
                    </div>
                </div>

                <div class="highlight">
                    <span class="persona">THE PERSONA</span>
                    <p id="personaTitle" class="title">최근 전적을 분석하고 있습니다.</p>
                </div>

                <div class="tags">
                    <span class="tag1" id="overviewWinRate">승률 -</span>
                    <span class="tag2" id="overviewAvgCs">평균 CS -</span>
                    <span class="tag3" id="overviewGames">게임 수 -</span>
                </div>
            </div>

            <div class="detail">
                <div class="details1">
                    <span class="detail1-1">평균 KDA</span>
                    <p class="detail1-2" id="avgKda">-</p>
                    <p class="detail1-3" id="avgKdaSub">최근 게임 기준 평균</p>
                </div>
                <div class="details2">
                    <span class="details2-1">승률</span>
                    <p class="details2-2" id="winRate">-</p>
                    <p class="details2-3" id="winLossText">-</p>
                </div>
                <div class="details3">
                    <span class="details3-1">평균 CS/min</span>
                    <p class="details3-2" id="avgCsPerMin">-</p>
                    <p class="details3-3">최근 게임 기준</p>
                </div>
            </div>

            <!-- 통계도 종합 페이지에 통합 -->
            <div class="overview-stats-grid">
                <div class="stat-card">
                    <span>총 킬</span>
                    <strong id="totalKills">-</strong>
                    <small>최근 전적 기준</small>
                </div>
                <div class="stat-card">
                    <span>총 어시스트</span>
                    <strong id="totalAssists">-</strong>
                    <small>최근 전적 기준</small>
                </div>
                <div class="stat-card">
                    <span>평균 CS</span>
                    <strong id="avgCs">-</strong>
                    <small>게임당 평균</small>
                </div>
                <div class="stat-card">
                    <span>최다 플레이 챔피언</span>
                    <strong id="mostPlayedChampion">-</strong>
                    <small id="mostPlayedChampionSub">-</small>
                </div>
                <div class="stat-card">
                    <span>주 포지션</span>
                    <strong id="mostPlayedPosition">-</strong>
                    <small id="mostPlayedPositionSub">-</small>
                </div>
                <div class="stat-card">
                    <span>최근 기록</span>
                    <strong id="recentRecord">-</strong>
                    <small>승 / 패</small>
                </div>
            </div>
        </div>

        <!-- 최근 전적 -->
        <div id="tabContent1" class="tab-content">
            <div class="section-header">
                <div>
                    <span class="analysis">MATCH HISTORY</span>
                    <h3>최근 전적</h3>
                </div>
                <span class="section-subtitle">경기를 선택해 팀 분석을 확인하세요.</span>
            </div>
            <div id="matchList" class="match-list">
                <div class="empty-state">플레이어를 검색하면 최근 전적이 표시됩니다.</div>
            </div>
        </div>

        <!-- 선택 경기 팀 분석 상세 -->
        <div id="matchDetailView" class="match-detail-view hidden">
            <div class="match-detail-topbar">
                <button class="back-button" type="button" onclick="backToHistory()">← 최근 전적으로</button>
                <div class="match-detail-heading">
                    <span class="analysis">MATCH ANALYSIS</span>
                    <h3>경기 상세 팀 분석</h3>
                </div>
                <div class="match-detail-actions">
                    <div id="matchDetailMeta" class="match-detail-meta">선택한 경기</div>
                    <button class="match-ai-top-button" type="button" onclick="startAIAnalysis()">✦ AI 분석</button>
                </div>
            </div>

            <div id="matchDetailContent" class="team-analysis-content">
                <div class="empty-state">최근 전적에서 경기를 선택해 주세요.</div>
            </div>
        </div>

        <section id="aiChatView" class="ai-chat-view hidden" aria-label="AI 경기 분석 채팅">
            <header class="ai-chat-topbar">
                <button class="back-button" type="button" onclick="closeAIChatView()">← 경기 분석으로</button>
                <div class="ai-chat-title">
                    <span class="analysis">AI MATCH COACH</span>
                    <h2>경기 취약점 분석</h2>
                </div>
                <div id="aiChatContext" class="ai-chat-context"></div>
            </header>
            <div id="aiChatStatus" class="ai-chat-status">최근 전적과 선택한 경기 기록을 분석합니다.</div>
            <div id="aiChatMessages" class="ai-chat-messages" aria-live="polite"></div>
            <form class="ai-chat-form" onsubmit="sendAIQuestion(event)">
                <input id="aiQuestionInput" type="text" maxlength="800" placeholder="분석 결과에 대해 질문해 보세요" autocomplete="off" disabled>
                <button id="aiSendButton" type="submit" disabled>전송</button>
            </form>
        </section>
    </main>

    <!-- 챔피언 상세 모달 -->
    <div id="championModal" class="champion-modal hidden" role="dialog" aria-modal="true" aria-labelledby="championModalTitle">
        <div class="champion-modal-backdrop" onclick="closeChampionModal()"></div>
        <div class="champion-modal-card">
            <button class="champion-modal-close" type="button" onclick="closeChampionModal()" aria-label="닫기">×</button>
            <div id="championModalContent" class="champion-modal-content">
                <div class="team-loading">챔피언 정보를 불러오는 중...</div>
            </div>
        </div>
    </div>

    <!-- 플레이어 전적 상세 모달 -->
    <div id="playerProfileModal" class="profile-modal hidden" role="dialog" aria-modal="true" aria-labelledby="playerProfileModalTitle">
        <div class="profile-modal-backdrop" onclick="closePlayerHistory()"></div>
        <div class="profile-modal-card">
            <button class="profile-modal-close" type="button" onclick="closePlayerHistory()" aria-label="닫기">×</button>
            <div id="playerProfileModalContent" class="profile-modal-content">
                <div class="team-loading">플레이어 전적을 불러오는 중...</div>
            </div>
        </div>
    </div>

    <script>
        let currentPlayerData = null;
        let selectedMatchAnalysisData = null;
        let aiConversation = [];
        let aiRequestPending = false;

        function escapeHtml(value) {
            return String(value ?? '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function cleanGameText(value) {
            return String(value ?? '')
                .replace(/<br\s*\/?>(?:\r?\n)?/gi, '\n')
                .replace(/<[^>]*>/g, '')
                .trim();
        }

        function formatDuration(seconds) {
            const mins = Math.floor((seconds || 0) / 60);
            const secs = Math.floor((seconds || 0) % 60);
            return `${mins}:${String(secs).padStart(2, '0')}`;
        }

        function formatDate(timestamp) {
            if (!timestamp) return '-';
            return new Date(timestamp).toLocaleString('ko-KR', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        function calculateSummary(matches) {
            if (!matches.length) {
                return {
                    games: 0, wins: 0, losses: 0, winRate: 0,
                    kills: 0, deaths: 0, assists: 0, kda: 0,
                    avgCs: 0, avgCsPerMin: 0
                };
            }

            const totals = matches.reduce((acc, match) => {
                acc.kills += Number(match.kills) || 0;
                acc.deaths += Number(match.deaths) || 0;
                acc.assists += Number(match.assists) || 0;
                acc.cs += Number(match.totalCs) || 0;
                acc.csPerMin += Number(match.csPerMinute) || 0;
                acc.wins += match.win ? 1 : 0;
                return acc;
            }, { kills: 0, deaths: 0, assists: 0, cs: 0, csPerMin: 0, wins: 0 });

            const games = matches.length;
            const losses = games - totals.wins;
            const kda = (totals.kills + totals.assists) / Math.max(totals.deaths, 1);

            return {
                games,
                wins: totals.wins,
                losses,
                winRate: (totals.wins / games) * 100,
                kills: totals.kills,
                deaths: totals.deaths,
                assists: totals.assists,
                kda,
                avgCs: totals.cs / games,
                avgCsPerMin: totals.csPerMin / games
            };
        }

        function getChampionIcon(championId) {
            return `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`;
        }

        function getMostPlayed(matches, key) {
            const map = new Map();
            for (const match of matches) {
                const value = match[key] || '미상';
                map.set(value, (map.get(value) || 0) + 1);
            }
            return [...map.entries()].sort((a, b) => b[1] - a[1])[0] || ['-', 0];
        }

        function renderOverview(data) {
            const matches = data.matches || [];
            const summary = calculateSummary(matches);
            const solo = (data.ranks || []).find(rank => rank.queueType === 'RANKED_SOLO_5x5');
            const [mostChampion, championCount] = getMostPlayed(matches, 'championName');
            const [mostPosition, positionCount] = getMostPlayed(matches, 'teamPosition');

            document.getElementById('userName').innerText = data.riotId;
            document.getElementById('userIcon').src =
                `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${data.profileIconId}.jpg`;
            document.getElementById('userIcon').alt = `${data.gameName} 프로필 아이콘`;

            document.getElementById('matchCountLabel').innerText = `최근 ${summary.games}게임 기준`;
            document.getElementById('kdaValue').innerText = summary.games ? `KDA ${summary.kda.toFixed(2)}` : 'KDA -';
            document.getElementById('avgKda').innerText = summary.games ? summary.kda.toFixed(2) : '-';
            document.getElementById('winRate').innerText = summary.games ? `${summary.winRate.toFixed(1)}%` : '-';
            document.getElementById('winLossText').innerText = summary.games ? `${summary.wins}승 ${summary.losses}패` : '-';
            document.getElementById('avgCsPerMin').innerText = summary.games ? summary.avgCsPerMin.toFixed(1) : '-';
            document.getElementById('overviewWinRate').innerText = `승률 ${summary.games ? summary.winRate.toFixed(1) : '-'}%`;
            document.getElementById('overviewAvgCs').innerText = `평균 CS ${summary.games ? summary.avgCs.toFixed(1) : '-'}`;
            document.getElementById('overviewGames').innerText = `${summary.games}게임`;

            document.getElementById('totalKills').innerText = summary.games ? summary.kills : '-';
            document.getElementById('totalAssists').innerText = summary.games ? summary.assists : '-';
            document.getElementById('avgCs').innerText = summary.games ? summary.avgCs.toFixed(1) : '-';
            const mostChampionEl = document.getElementById('mostPlayedChampion');
            const mostChampionMatch = matches.find(match => match.championName === mostChampion && match.championId);
            mostChampionEl.innerText = mostChampion;
            mostChampionEl.classList.toggle('clickable-stat', Boolean(mostChampionMatch));
            mostChampionEl.onclick = mostChampionMatch ? () => openChampionInfo(mostChampionMatch.championId) : null;
            if (mostChampionMatch) mostChampionEl.title = `${mostChampion} 챔피언 정보 보기`;
            document.getElementById('mostPlayedChampionSub').innerText = `${championCount}게임`;
            document.getElementById('mostPlayedPosition').innerText = mostPosition;
            document.getElementById('mostPlayedPositionSub').innerText = `${positionCount}게임`;
            document.getElementById('recentRecord').innerText = summary.games ? `${summary.wins}승 ${summary.losses}패` : '-';

            const userRank = document.getElementById('userRank');
            const tierIcon = document.getElementById('tierIcon');
            if (solo) {
                userRank.innerText = `${solo.tier} ${solo.rank} · ${solo.leaguePoints} LP · Lv.${data.summonerLevel}`;
                tierIcon.src = `https://opgg-static.akamaized.net/images/medals_new/${solo.tier.toLowerCase()}.png`;
                tierIcon.style.display = 'inline-block';
                tierIcon.alt = `${solo.tier} 아이콘`;
            } else {
                userRank.innerText = `언랭크 · Lv.${data.summonerLevel}`;
                tierIcon.style.display = 'none';
            }

            const title = document.getElementById('personaTitle');
            if (!summary.games) title.innerText = '최근 전적을 찾을 수 없습니다.';
            else if (summary.winRate >= 60) title.innerText = '최근 승률이 높은 안정적인 플레이어';
            else if (summary.kda >= 3) title.innerText = '교전 기여도가 높은 플레이어';
            else if (summary.avgCsPerMin >= 7) title.innerText = '성장과 CS 수급을 중시하는 플레이어';
            else title.innerText = '최근 게임 흐름을 꾸준히 이어가는 플레이어';
        }

        function renderMatchHistory(matches) {
            const matchList = document.getElementById('matchList');
            matchList.innerHTML = '';

            if (!matches.length) {
                matchList.innerHTML = '<div class="empty-state">최근 전적 데이터가 없습니다.</div>';
                return;
            }

            for (const match of matches) {
                const resultClass = match.win ? 'win' : 'loss';
                const resultText = match.win ? '승리' : '패배';
                const championIcon = getChampionIcon(match.championId);
                const kda = `${match.kills} / ${match.deaths} / ${match.assists}`;
                const kdaRatio = ((match.kills + match.assists) / Math.max(match.deaths, 1)).toFixed(2);

                const row = document.createElement('div');
                row.className = 'match-history-action-row';
                const item = document.createElement('button');
                item.type = 'button';
                item.className = `match-card ${resultClass}`;
                item.setAttribute('aria-label', `${match.championName} ${resultText} 경기 팀 분석 보기`);
                item.onclick = () => openMatchDetail(match.matchId);
                item.innerHTML = `
                    <div class="match-result">
                        <strong>${resultText}</strong>
                        <span>${escapeHtml(match.queueName)}</span>
                    </div>
                    <div class="champion-cell">
                        <img src="${championIcon}" alt="${escapeHtml(match.championName)}" onerror="this.style.display='none'">
                        <div>
                            <strong>${escapeHtml(match.championName || '알 수 없음')}</strong>
                            <span>${escapeHtml(match.teamPosition || '포지션 미상')}</span>
                        </div>
                    </div>
                    <div class="kda-cell">
                        <strong>${kda}</strong>
                        <span>KDA ${kdaRatio}</span>
                    </div>
                    <div class="cs-cell">
                        <strong>${match.totalCs} CS</strong>
                        <span>${match.csPerMinute} CS/min</span>
                    </div>
                    <div class="match-meta">
                        <strong>${formatDuration(match.gameDuration)}</strong>
                        <span>${formatDate(match.gameCreation)}</span>
                        <small>클릭하여 팀 분석</small>
                    </div>
                `;
                row.append(item);
                matchList.appendChild(row);
            }
        }

        function renderPlayerRow(player, isMine) {
            const kda = `${player.kills}/${player.deaths}/${player.assists}`;
            const kdaRatio = ((player.kills + player.assists) / Math.max(player.deaths, 1)).toFixed(2);
            const champId = Number(player.championId) || 0;
            return `
                <div class="team-player-row ${isMine ? 'mine' : ''}">
                    <button class="team-champion-button" type="button" onclick="openChampionInfo(${champId})" title="${escapeHtml(player.championName || '챔피언')} 정보 보기">
                        <img class="team-champion-icon" src="${getChampionIcon(champId)}" alt="${escapeHtml(player.championName || '챔피언')}" onerror="this.style.display='none'">
                    </button>
                    <div class="team-player-main">
                        <div class="team-player-name-line">
                            <button class="team-player-name-button" type="button" onclick="openPlayerHistory('${escapeHtml(player.puuid)}')" title="이 플레이어의 최근 전적 보기">
                                <strong>${escapeHtml(player.gameName)}</strong>
                            </button>
                            ${isMine ? '<span class="mine-badge">MY PLAYER</span>' : ''}
                        </div>
                        <span>${escapeHtml(player.teamPosition)} · ${escapeHtml(player.rankLabel || '언랭크')}</span>
                    </div>
                    <button class="team-champion-name-button" type="button" onclick="openChampionInfo(${champId})" title="챔피언 정보 보기">
                        ${escapeHtml(player.championName || '알 수 없음')}
                    </button>
                    <span class="team-title-badge">${escapeHtml(player.title)}</span>
                    <div class="team-stat-block">
                        <strong>${kda}</strong>
                        <span>KDA ${kdaRatio}</span>
                    </div>
                    <div class="team-stat-block compact">
                        <strong>${player.totalCs}</strong>
                        <span>CS</span>
                    </div>
                    <div class="team-stat-block compact">
                        <strong>${player.killParticipation}%</strong>
                        <span>KP</span>
                    </div>
                </div>
            `;
        }

        function renderTeamAnalysis(data) {
            const root = document.getElementById('matchDetailContent');
            const meta = document.getElementById('matchDetailMeta');
            if (!root) return;

            selectedMatchAnalysisData = data;
            aiConversation = [];

            meta.innerText = `${data.queueName} · ${formatDate(data.gameCreation)} · ${formatDuration(data.gameDuration)}`;

            const myProfile = data.myProfile;
            const enemyProfile = data.enemyProfile;
            const myPlayers = data.myTeam || [];
            const enemyPlayers = data.enemyTeam || [];
            const prediction = data.winEstimate || { available: false, myWinRate: 50, enemyWinRate: 50 };

            root.innerHTML = `
                <section class="win-prediction-card">
                    <div class="prediction-header">
                        <div>
                            <span class="analysis">TEAM POWER ESTIMATE</span>
                            <h4>경기 전 전력 추정</h4>
                        </div>
                        <span class="prediction-badge">랭크 기반</span>
                    </div>

                    <div class="prediction-score-row">
                        <div class="prediction-team prediction-team-mine">
                            <span>우리 팀</span>
                            <strong>${prediction.myWinRate}%</strong>
                            <small>예상 우세도</small>
                        </div>
                        <div class="prediction-center">
                            <div class="prediction-bar">
                                <div class="prediction-bar-my" style="width:${prediction.myWinRate}%;"></div>
                            </div>
                            <span>VS</span>
                        </div>
                        <div class="prediction-team prediction-team-enemy">
                            <span>상대 팀</span>
                            <strong>${prediction.enemyWinRate}%</strong>
                            <small>예상 우세도</small>
                        </div>
                    </div>

                    <div class="prediction-meta">
                        <div><span>우리 평균 전력</span><strong>${prediction.myStrength?.avgScore ?? '-'} / 17</strong></div>
                        <div><span>상대 평균 전력</span><strong>${prediction.enemyStrength?.avgScore ?? '-'} / 17</strong></div>
                        <div><span>랭크 확인</span><strong>${prediction.myStrength?.rankedPlayers ?? 0} vs ${prediction.enemyStrength?.rankedPlayers ?? 0}</strong></div>
                    </div>

                    <p class="prediction-note">${escapeHtml(prediction.note || '랭크 기반 전력 추정치입니다.')}</p>
                </section>

                <div class="team-columns">
                    <section class="team-panel my-team-panel">
                        <div class="team-panel-header">
                            <div>
                                <span class="analysis">MY TEAM</span>
                                <h4>우리 팀</h4>
                            </div>
                            <span class="team-profile-chip">${escapeHtml(myProfile.profile)}</span>
                        </div>
                        <div class="team-summary-row">
                            <div><span>평균 CS/min</span><strong>${myProfile.avgCsPerMin}</strong></div>
                            <div><span>평균 KP</span><strong>${myProfile.avgKillParticipation}%</strong></div>
                            <div><span>평균 데스</span><strong>${myProfile.avgDeaths}</strong></div>
                        </div>
                        <div class="team-player-list">
                            ${myPlayers.map(p => renderPlayerRow(p, p.puuid === currentPlayerData?.puuid)).join('')}
                        </div>
                    </section>

                    <section class="team-panel enemy-team-panel">
                        <div class="team-panel-header">
                            <div>
                                <span class="analysis">OPPONENT TEAM</span>
                                <h4>상대 팀</h4>
                            </div>
                            <span class="team-profile-chip enemy-chip">${escapeHtml(enemyProfile.profile)}</span>
                        </div>
                        <div class="team-summary-row">
                            <div><span>평균 CS/min</span><strong>${enemyProfile.avgCsPerMin}</strong></div>
                            <div><span>평균 KP</span><strong>${enemyProfile.avgKillParticipation}%</strong></div>
                            <div><span>평균 데스</span><strong>${enemyProfile.avgDeaths}</strong></div>
                        </div>
                        <div class="team-player-list">
                            ${enemyPlayers.map(p => renderPlayerRow(p, false)).join('')}
                        </div>
                    </section>
                </div>

                <section class="team-tip-card">
                    <div class="team-tip-heading">
                        <span>💡 TEAM TIP</span>
                        <small>선택 경기 복기</small>
                    </div>
                    <div class="tip-list">
                        ${(data.tips || []).map(tip => `<p>${escapeHtml(tip)}</p>`).join('')}
                    </div>
                </section>

                <div class="objectives-grid">
                    <div class="objective-card">
                        <span>우리 팀 오브젝트</span>
                        <strong>Dragon ${data.myObjectives.dragon} · Baron ${data.myObjectives.baron}</strong>
                        <small>Tower ${data.myObjectives.tower} · Inhibitor ${data.myObjectives.inhibitor}</small>
                    </div>
                    <div class="objective-card">
                        <span>상대 팀 오브젝트</span>
                        <strong>Dragon ${data.enemyObjectives.dragon} · Baron ${data.enemyObjectives.baron}</strong>
                        <small>Tower ${data.enemyObjectives.tower} · Inhibitor ${data.enemyObjectives.inhibitor}</small>
                    </div>
                </div>

            `;
        }

        function appendAIMessage(role, text) {
            const messages = document.getElementById('aiChatMessages');
            if (!messages) return;
            const message = document.createElement('div');
            message.className = `ai-chat-message ${role === 'user' ? 'ai-chat-user' : 'ai-chat-assistant'}`;
            message.innerHTML = escapeHtml(text).replace(/\r?\n/g, '<br>');
            messages.appendChild(message);
            messages.scrollTop = messages.scrollHeight;
        }

        function openAIChatView() {
            document.getElementById('mainNavigation').classList.add('hidden');
            document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
            document.getElementById('matchDetailView').classList.add('hidden');
            document.getElementById('aiChatView').classList.remove('hidden');
            const context = document.getElementById('aiChatContext');
            if (context) {
                const match = selectedMatchAnalysisData || {};
                context.textContent = `${currentPlayerData?.riotId || '소환사'} · ${match.queueName || '선택 경기'}`;
            }
        }

        function closeAIChatView() {
            document.getElementById('aiChatView').classList.add('hidden');
            document.getElementById('matchDetailView').classList.remove('hidden');
        }

        function aiAnalysisPayload(mode, question = '') {
            return {
                mode,
                question,
                riotId: currentPlayerData?.riotId || '',
                recentMatches: currentPlayerData?.matches || [],
                selectedMatch: selectedMatchAnalysisData,
                history: mode === 'chat' ? aiConversation.slice(0, -1) : aiConversation
            };
        }

        async function requestAIAnalysis(mode, question = '') {
            const response = await fetch('/api/ai-analysis', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(aiAnalysisPayload(mode, question))
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'AI 분석 요청에 실패했습니다.');
            return data.reply || '';
        }

        async function startAIAnalysis() {
            if (aiRequestPending || !selectedMatchAnalysisData || !currentPlayerData) return;
            openAIChatView();
            const status = document.getElementById('aiChatStatus');
            const input = document.getElementById('aiQuestionInput');
            const sendButton = document.getElementById('aiSendButton');
            const messages = document.getElementById('aiChatMessages');
            aiRequestPending = true;
            aiConversation = [];
            messages.innerHTML = '';
            status.textContent = '최근 전적과 경기 상세를 함께 분석하고 있습니다...';
            appendAIMessage('model', '반복되는 취약점과 다음 경기에서 시도할 행동을 찾고 있습니다.');
            input.disabled = true;
            sendButton.disabled = true;
            try {
                const reply = await requestAIAnalysis('analyze');
                if (messages.lastElementChild) messages.removeChild(messages.lastElementChild);
                aiConversation.push({ role: 'model', text: reply });
                status.textContent = `${currentPlayerData.matches?.length || 0}개 최근 경기와 선택 경기를 바탕으로 분석했습니다.`;
                appendAIMessage('model', reply);
                input.disabled = false;
                sendButton.disabled = false;
                input.focus();
            } catch (error) {
                status.textContent = error.message || 'AI 분석을 가져오지 못했습니다.';
                if (messages.lastElementChild) messages.removeChild(messages.lastElementChild);
                appendAIMessage('model', error.message || 'AI 분석을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            } finally {
                aiRequestPending = false;
            }
        }

        async function sendAIQuestion(event) {
            event.preventDefault();
            if (aiRequestPending || !aiConversation.length) return;
            const input = document.getElementById('aiQuestionInput');
            const button = document.getElementById('aiSendButton');
            const question = input.value.trim();
            if (!question) return;
            aiConversation.push({ role: 'user', text: question });
            appendAIMessage('user', question);
            input.value = '';
            aiRequestPending = true;
            if (button) button.disabled = true;
            appendAIMessage('model', '기록을 바탕으로 답변을 작성하고 있습니다...');
            const messages = document.getElementById('aiChatMessages');
            try {
                const reply = await requestAIAnalysis('chat', question);
                if (messages?.lastElementChild) messages.removeChild(messages.lastElementChild);
                aiConversation.push({ role: 'model', text: reply });
                appendAIMessage('model', reply);
            } catch (error) {
                if (messages?.lastElementChild) messages.removeChild(messages.lastElementChild);
                appendAIMessage('model', error.message || 'AI 답변을 가져오지 못했습니다.');
                aiConversation.pop();
            } finally {
                aiRequestPending = false;
                if (button) button.disabled = false;
                input.focus();
            }
        }

        async function openChampionInfo(championId) {
            const modal = document.getElementById('championModal');
            const content = document.getElementById('championModalContent');
            if (!modal || !content || !championId) return;

            modal.classList.remove('hidden');
            document.body.classList.add('modal-open');
            content.innerHTML = '<div class="team-loading">챔피언 정보를 불러오는 중...</div>';

            try {
                const response = await fetch(`/api/champion/${encodeURIComponent(championId)}`);
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || '챔피언 정보를 가져오지 못했습니다.');

                const tags = (data.tags || []).map(tag => `<span>${escapeHtml(tag)}</span>`).join('');
                const abilities = (data.spells || []).map((spell, index) => `
                    <article class="champion-ability">
                        <img src="${escapeHtml(spell.imageUrl || '')}" alt="${escapeHtml(spell.name || '')}">
                        <div>
                            <span>${['Q','W','E','R'][index] || ''}</span>
                            <h4>${escapeHtml(spell.name || '')}</h4>
                            <p>${escapeHtml(spell.description || '')}</p>
                        </div>
                    </article>
                `).join('');

                content.innerHTML = `
                    <div class="champion-hero">
                        <img class="champion-splash" src="${escapeHtml(data.splashUrl || data.imageUrl || '')}" alt="${escapeHtml(data.name)}">
                        <div class="champion-hero-overlay"></div>
                        <div class="champion-hero-copy">
                            <span class="analysis">CHAMPION INFO</span>
                            <h2 id="championModalTitle">${escapeHtml(data.name)}</h2>
                            <p>${escapeHtml(data.title || '')}</p>
                            <div class="champion-tag-list">${tags}</div>
                        </div>
                    </div>

                    <div class="champion-info-grid">
                        <section class="champion-info-card champion-lore-card">
                            <span class="analysis">ABOUT</span>
                            <h3>챔피언 소개</h3>
                            <p>${escapeHtml(data.blurb || data.lore || '소개 정보가 없습니다.')}</p>
                        </section>
                        <section class="champion-info-card">
                            <span class="analysis">BASE STATS</span>
                            <h3>기본 능력치</h3>
                            <div class="champion-stat-lines">
                                <div><span>체력</span><strong>${Math.round(data.stats?.hp || 0)}</strong></div>
                                <div><span>공격력</span><strong>${Math.round(data.stats?.attackdamage || 0)}</strong></div>
                                <div><span>방어력</span><strong>${Math.round(data.stats?.armor || 0)}</strong></div>
                                <div><span>사거리</span><strong>${Math.round(data.stats?.attackrange || 0)}</strong></div>
                            </div>
                        </section>
                    </div>

                    <section class="champion-abilities-card">
                        <div class="champion-section-heading">
                            <div>
                                <span class="analysis">ABILITIES</span>
                                <h3>스킬</h3>
                            </div>
                            <span class="champion-passive-label">패시브 + Q W E R</span>
                        </div>
                        <div class="champion-ability-list">
                            <article class="champion-ability">
                                <img src="${escapeHtml(data.passive?.imageUrl || '')}" alt="${escapeHtml(data.passive?.name || '패시브')}">
                                <div><span>P</span><h4>${escapeHtml(data.passive?.name || '패시브')}</h4><p>${escapeHtml(data.passive?.description || '')}</p></div>
                            </article>
                            ${abilities}
                        </div>
                    </section>
                `;
            } catch (error) {
                console.error(error);
                content.innerHTML = `<div class="empty-state">${escapeHtml(error.message || '챔피언 정보를 불러오지 못했습니다.')}</div>`;
            }
        }

        function renderPlayerHistory(profile) {
            const content = document.getElementById('playerProfileModalContent');
            if (!content) return;

            const matches = profile.matches || [];
            const solo = (profile.ranks || []).find(rank => rank.queueType === 'RANKED_SOLO_5x5');
            const summary = calculateSummary(matches);
            const recent = matches.slice(0, 10);

            content.innerHTML = `
                <div class="profile-hero">
                    <img class="profile-hero-icon" src="https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${Number(profile.profileIconId) || 0}.jpg" alt="${escapeHtml(profile.riotId || '플레이어')}" onerror="this.style.display='none'">
                    <div>
                        <span class="analysis">PLAYER HISTORY</span>
                        <h2 id="playerProfileModalTitle">${escapeHtml(profile.riotId || '플레이어')}</h2>
                        <p>${escapeHtml(solo ? `${solo.tier} ${solo.rank} · ${solo.leaguePoints} LP` : '언랭크')} · Lv.${Number(profile.summonerLevel) || 0}</p>
                    </div>
                </div>

                <div class="profile-summary-grid">
                    <div><span>최근 승률</span><strong>${summary.games ? `${summary.winRate.toFixed(1)}%` : '-'}</strong><small>${summary.wins}승 ${summary.losses}패</small></div>
                    <div><span>KDA</span><strong>${summary.games ? summary.kda.toFixed(2) : '-'}</strong><small>${summary.kills}/${summary.deaths}/${summary.assists}</small></div>
                    <div><span>평균 CS/min</span><strong>${summary.games ? summary.avgCsPerMin.toFixed(1) : '-'}</strong><small>최근 ${summary.games}게임</small></div>
                    <div><span>평균 CS</span><strong>${summary.games ? summary.avgCs.toFixed(1) : '-'}</strong><small>게임당 평균</small></div>
                </div>

                <section class="profile-history-section">
                    <div class="section-header">
                        <div><span class="analysis">RECENT MATCHES</span><h3>최근 전적</h3></div>
                        <span class="section-subtitle">${summary.games}게임</span>
                    </div>
                    <div class="profile-match-list">
                        ${recent.length ? recent.map(match => {
                            const resultClass = match.win ? 'win' : 'loss';
                            return `
                                <div class="profile-match-row ${resultClass}">
                                    <div class="profile-match-result"><strong>${match.win ? '승리' : '패배'}</strong><span>${escapeHtml(match.queueName || '')}</span></div>
                                    <button class="profile-champion-button" type="button" onclick="openChampionInfo(${Number(match.championId) || 0})">
                                        <img src="${getChampionIcon(match.championId)}" alt="${escapeHtml(match.championName || '')}">
                                        <span>${escapeHtml(match.championName || '알 수 없음')}</span>
                                    </button>
                                    <div class="profile-kda"><strong>${match.kills}/${match.deaths}/${match.assists}</strong><span>KDA ${((match.kills + match.assists) / Math.max(match.deaths, 1)).toFixed(2)}</span></div>
                                    <div class="profile-cs"><strong>${match.totalCs}</strong><span>CS · ${match.csPerMinute}/min</span></div>
                                    <div class="profile-match-time"><strong>${formatDuration(match.gameDuration)}</strong><span>${formatDate(match.gameCreation)}</span></div>
                                </div>
                            `;
                        }).join('') : '<div class="empty-state">최근 전적이 없습니다.</div>'}
                    </div>
                </section>
            `;
        }

        async function openPlayerHistory(puuid) {
            const modal = document.getElementById('playerProfileModal');
            const content = document.getElementById('playerProfileModalContent');
            if (!modal || !content || !puuid) return;

            modal.classList.remove('hidden');
            document.body.classList.add('modal-open');
            content.innerHTML = '<div class="team-loading">플레이어 전적을 불러오는 중...</div>';

            try {
                const response = await fetch(`/api/player-profile?puuid=${encodeURIComponent(puuid)}`);
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || '플레이어 전적을 가져오지 못했습니다.');
                renderPlayerHistory(data);
            } catch (error) {
                console.error(error);
                content.innerHTML = `<div class="empty-state">${escapeHtml(error.message || '플레이어 전적을 불러오지 못했습니다.')}</div>`;
            }
        }

        function closePlayerHistory() {
            const modal = document.getElementById('playerProfileModal');
            if (!modal) return;
            modal.classList.add('hidden');
            document.body.classList.remove('modal-open');
        }

        function closeAllModals() {
            closeChampionModal();
            closePlayerHistory();
        }

        function closeChampionModal() {
            const modal = document.getElementById('championModal');
            if (!modal) return;
            modal.classList.add('hidden');
            document.body.classList.remove('modal-open');
        }

        function renderLivePlayer(player, isMine) {
            const icon = getChampionIcon(player.championId);
            const recentCount = Number(player.recentGames) || 0;
            const recentLabel = recentCount ? `${Number(player.recentWinRate).toFixed(0)}% (${player.recentWins}승 ${player.recentLosses}패)` : '전적 부족';
            const seasonCount = (Number(player.seasonWins) || 0) + (Number(player.seasonLosses) || 0);
            const seasonLabel = seasonCount ? `${Number(player.seasonWinRate).toFixed(0)}% (${player.seasonWins}승 ${player.seasonLosses}패)` : '언랭크';
            return `
                <article class="live-player-row ${isMine ? 'mine' : ''}">
                    <img class="live-champion-icon" src="${icon}" alt="${escapeHtml(player.championName || '')}">
                    <div class="live-player-main">
                        <strong>${escapeHtml(player.riotId || player.gameName || '플레이어')}</strong>
                        <span>${escapeHtml(player.teamPosition || '포지션 미상')}${player.positionEstimated ? ' · 주 포지션 추정' : ''} · ${escapeHtml(player.rankLabel || '언랭크')}</span>
                        <small>${escapeHtml(player.championName || '알 수 없음')} · 최근 ${recentCount}경기 ${recentLabel}</small>
                    </div>
                    <div class="live-player-record"><strong>${seasonLabel}</strong><span>솔로랭크 승률</span></div>
                    <div class="live-player-record"><strong>${Number(player.recentKda || 0).toFixed(2)}</strong><span>최근 KDA</span></div>
                </article>
            `;
        }

        function renderLiveGame(data) {
            const root = document.getElementById('liveGameContent');
            if (!root) return;
            if (!data.available) {
                root.innerHTML = `<div class="empty-state live-empty">${escapeHtml(data.message || '현재 진행 중인 게임을 찾을 수 없습니다.')}</div>`;
                return;
            }

            const teamEstimate = data.teamEstimate || { myWinRate: 50, enemyWinRate: 50 };
            const myWinRate = Math.max(0, Math.min(100, Number(teamEstimate.myWinRate) || 50));
            const enemyWinRate = 100 - myWinRate;
            const ownTeam = data.myTeam || [];
            const enemyTeam = data.enemyTeam || [];
            const lanes = data.laneEstimates || [];
            const laneMarkup = lanes.length ? lanes.map(lane => `
                <article class="live-lane-row">
                    <div class="live-lane-player"><strong>${escapeHtml(lane.myPlayer.teamPosition)} · ${escapeHtml(lane.myPlayer.championName)}</strong><span>${escapeHtml(lane.myPlayer.riotId)} · ${escapeHtml(lane.myPlayer.rankLabel)}</span><small>최근 ${Number(lane.myPlayer.recentWinRate).toFixed(0)}% · KDA ${Number(lane.myPlayer.recentKda).toFixed(2)}</small></div>
                    <div class="live-lane-result"><span>${escapeHtml(lane.position)}</span><strong>${lane.myWinRate}% : ${lane.enemyWinRate}%</strong><small>라인 우세 추정</small></div>
                    <div class="live-lane-player enemy"><strong>${escapeHtml(lane.enemyPlayer.teamPosition)} · ${escapeHtml(lane.enemyPlayer.championName)}</strong><span>${escapeHtml(lane.enemyPlayer.riotId)} · ${escapeHtml(lane.enemyPlayer.rankLabel)}</span><small>최근 ${Number(lane.enemyPlayer.recentWinRate).toFixed(0)}% · KDA ${Number(lane.enemyPlayer.recentKda).toFixed(2)}</small></div>
                </article>
            `).join('') : '<div class="empty-state">최근 전적에서 양 팀의 같은 포지션을 확인하지 못해 라인별 비교를 만들 수 없습니다.</div>';

            root.innerHTML = `
                <div class="live-game-meta">${escapeHtml(data.queueName || data.gameMode || '현재 게임')} · 진행 ${formatDuration(data.gameLength || 0)} · 참가자 ${ownTeam.length + enemyTeam.length}명</div>
                <section class="live-team-estimate">
                    <div class="live-estimate-heading"><div><span class="analysis">TEAM VS TEAM</span><h4>팀 대 팀 우세도</h4></div><span class="prediction-badge">최근 전적 기반 추정</span></div>
                    <div class="prediction-score-row">
                        <div class="prediction-team prediction-team-mine"><span>내 팀</span><strong>${myWinRate}%</strong><small>예상 우세도</small></div>
                        <div class="prediction-center"><div class="prediction-bar"><div class="prediction-bar-my" style="width:${myWinRate}%;"></div></div><span>VS</span></div>
                        <div class="prediction-team prediction-team-enemy"><span>상대 팀</span><strong>${enemyWinRate}%</strong><small>예상 우세도</small></div>
                    </div>
                    <div class="live-estimate-sub"><span>팀 평균 최근 승률 ${Number(teamEstimate.myRecentWinRate || 0).toFixed(1)}%</span><span>상대 팀 평균 ${Number(teamEstimate.enemyRecentWinRate || 0).toFixed(1)}%</span></div>
                    <p class="prediction-note">${escapeHtml(data.note || '')}</p>
                </section>

                <section class="live-lane-section"><div class="live-section-heading"><div><span class="analysis">LANE MATCHUPS</span><h4>라인 대 라인 전적 비교</h4></div><span>주 포지션과 솔로랭크·최근 전적 기준</span></div><div class="live-lane-list">${laneMarkup}</div></section>

                <div class="team-columns live-team-columns">
                    <section class="team-panel my-team-panel"><div class="team-panel-header"><div><span class="analysis">MY TEAM</span><h4>내 팀 · ${ownTeam.length}명</h4></div></div><div class="live-player-list">${ownTeam.map(p => renderLivePlayer(p, p.puuid === currentPlayerData?.puuid)).join('')}</div></section>
                    <section class="team-panel enemy-team-panel"><div class="team-panel-header"><div><span class="analysis">OPPONENT TEAM</span><h4>상대 팀 · ${enemyTeam.length}명</h4></div></div><div class="live-player-list">${enemyTeam.map(p => renderLivePlayer(p, false)).join('')}</div></section>
                </div>
            `;
        }

        async function loadLiveGame(forceRefresh = false) {
            const root = document.getElementById('liveGameContent');
            if (!root) return;
            if (!currentPlayerData?.puuid) {
                root.innerHTML = '<div class="empty-state">플레이어를 먼저 검색해 주세요.</div>';
                return;
            }
            root.innerHTML = '<div class="team-loading">현재 게임과 참가자 전적을 확인하고 있습니다...</div>';
            try {
                const response = await fetch(`/api/live-game?puuid=${encodeURIComponent(currentPlayerData.puuid)}&summonerId=${encodeURIComponent(currentPlayerData.summonerId || '')}&refresh=${forceRefresh ? Date.now() : ''}`);
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || '현재 게임 정보를 가져오지 못했습니다.');
                renderLiveGame(data);
            } catch (error) {
                console.error(error);
                root.innerHTML = `<div class="empty-state">${escapeHtml(error.message || '현재 게임 정보를 불러오지 못했습니다.')}</div>`;
            }
        }

        async function openMatchDetail(matchId, autoAnalyze = false) {
            if (!matchId || !currentPlayerData?.puuid) return;

            const nav = document.getElementById('mainNavigation');
            const detail = document.getElementById('matchDetailView');
            const root = document.getElementById('matchDetailContent');

            nav.classList.add('hidden');
            document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
            detail.classList.remove('hidden');
            root.innerHTML = '<div class="team-loading">선택한 경기의 10명과 팀 분석 데이터를 불러오는 중...</div>';

            window.scrollTo({ top: 0, behavior: 'smooth' });

            try {
                const response = await fetch(`/api/team?matchId=${encodeURIComponent(matchId)}&puuid=${encodeURIComponent(currentPlayerData.puuid)}`);
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || '경기 팀 분석 데이터를 가져오지 못했습니다.');
                renderTeamAnalysis(data);
                if (autoAnalyze) startAIAnalysis();
            } catch (error) {
                console.error(error);
                root.innerHTML = `<div class="empty-state">${escapeHtml(error.message || '경기 분석 중 오류가 발생했습니다.')}</div>`;
            }
        }

        function backToHistory() {
            const nav = document.getElementById('mainNavigation');
            const overview = document.getElementById('tabContent0');
            const history = document.getElementById('tabContent1');
            const detail = document.getElementById('matchDetailView');

            detail.classList.add('hidden');
            nav.classList.remove('hidden');
            overview.classList.remove('active');
            history.classList.add('active');
            document.querySelectorAll('.result-tab').forEach((tab, i) => {
                tab.classList.toggle('active', i === 1);
            });
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }

        async function searchPersona() {
            const input = document.getElementById('summonerInput').value.trim();
            const dashboard = document.getElementById('resultDashboard');
            const button = document.querySelector('.search-button');

            if (!input) {
                alert('Riot ID를 입력해 주세요. 예: Hide on bush#KR1');
                return;
            }

            if (button) {
                button.disabled = true;
                button.textContent = '검색 중...';
            }

            try {
                const response = await fetch(`/api/player?riotId=${encodeURIComponent(input)}`);
                const data = await response.json();

                if (!response.ok) throw new Error(data.message || '플레이어 정보를 가져오지 못했습니다.');

                currentPlayerData = data;
                renderOverview(data);
                renderMatchHistory(data.matches || []);
                document.getElementById('matchDetailView').classList.add('hidden');
                document.getElementById('mainNavigation').classList.remove('hidden');
                showTab(0);
                dashboard.classList.remove('hidden');
            } catch (error) {
                console.error(error);
                alert(error.message || '오류가 발생했습니다.');
                dashboard.classList.add('hidden');
            } finally {
                if (button) {
                    button.disabled = false;
                    button.textContent = '성향 분석';
                }
            }
        }

        const header = document.querySelector('.top');
        const searchBox = document.querySelector('.search-box');
        header.appendChild(searchBox);
        header.classList.add('search-complete');
        searchBox.classList.add('compact');

        function showTab(index) {
            const detail = document.getElementById('matchDetailView');
            detail.classList.add('hidden');
            document.getElementById('aiChatView').classList.add('hidden');
            document.getElementById('mainNavigation').classList.remove('hidden');

            document.querySelectorAll('.result-tab').forEach((tab, i) => {
                tab.classList.toggle('active', i === index);
            });
            document.querySelectorAll('.tab-content').forEach((content, i) => {
                content.classList.toggle('active', i === index);
            });

        }

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') closeAllModals();
        });

        document.getElementById('summonerInput').addEventListener('keydown', (event) => {
            if (event.key === 'Enter') searchPersona();
        });
    </script>
</body>

</html>

