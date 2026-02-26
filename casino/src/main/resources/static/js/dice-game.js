document.addEventListener('DOMContentLoaded', () => {
    const rollButton = document.getElementById('roll-dice-btn');
    const spinner = document.getElementById('spinner');
    const resultArea = document.getElementById('game-result');
    const errorArea = document.getElementById('game-error');

    formatTimestamps();

    rollButton.addEventListener('click', async () => {
        try {
            rollButton.disabled = true;
            spinner.classList.remove('hidden');
            resultArea.classList.add('hidden');
            errorArea.classList.add('hidden');

            const clientNonce = generateNonce();
            const clientNonceHash = await computeHash(clientNonce);

            const csrfElement = document.getElementById('csrf-token');
            const csrfToken = csrfElement.dataset.token;
            const csrfHeader = csrfElement.dataset.header;

            const initiateResponse = await fetch('/game', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ clientNonceHash })
            });

            if (!initiateResponse.ok) {
                window.location.href = '/error';
                return;
            }

            const { gameId, serverNonceHash } = await initiateResponse.json();

            const revealResponse = await fetch(`/game/${gameId}/reveal`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ clientNonce })
            });

            if (!revealResponse.ok) {
                window.location.href = '/error';
                return;
            }

            const { gameOutcome, serverRoll, clientRoll, serverNonce } = await revealResponse.json();

            const isValid = await verifyServerNonceAndRolls(serverNonce, serverNonceHash, clientNonce, serverRoll, clientRoll);
            if (!isValid) {
                showError('Server cheating detected! Commitment verification failed.');
                return;
            }

            showResult(clientRoll, serverRoll, gameOutcome);
            updateGameHistory(clientRoll, serverRoll, gameOutcome);
        } catch (error) {
            window.location.href = '/error';
        } finally {
            spinner.classList.add('hidden');
            rollButton.disabled = false;
        }
    });

    function generateNonce() {
        const array = new Uint8Array(32);
        crypto.getRandomValues(array);
        return Array.from(array)
            .map(b => b.toString(16).padStart(2, '0'))
            .join('');
    }

    async function computeHash(input) {
        const encoder = new TextEncoder();
        const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(input));
        return Array.from(new Uint8Array(hashBuffer))
            .map(b => b.toString(16).padStart(2, '0'))
            .join('');
    }

    async function deriveRoll(role, serverNonce, clientNonce) {
        const data = role + serverNonce + clientNonce;
        const encoder = new TextEncoder();
        const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(data));
        const hashBytes = new Uint8Array(hashBuffer);
        const uint32 = ((hashBytes[0] << 24) | (hashBytes[1] << 16) | (hashBytes[2] << 8) | hashBytes[3]) >>> 0;
        return (uint32 % 6) + 1;
    }

    async function verifyServerNonceAndRolls(serverNonce, serverNonceHash, clientNonce, serverRoll, clientRoll) {
        const computedServerNonceHash = await computeHash(serverNonce);
        if (computedServerNonceHash !== serverNonceHash) return false;

        const computedServerRoll = await deriveRoll('server', serverNonce, clientNonce);
        const computedClientRoll = await deriveRoll('client', serverNonce, clientNonce);
        return computedServerRoll === serverRoll && computedClientRoll === clientRoll;
    }

    function showResult(clientRoll, serverRoll, gameOutcome) {
        const outcomeText =
            gameOutcome === 'CLIENT_WIN' ? 'You Won!' :
            gameOutcome === 'SERVER_WIN' ? 'You Lost!' :
            gameOutcome === 'TIE' ? 'It\'s a Tie!' :
            'Game Expired!';

        resultArea.replaceChildren();

        const outcomeDiv = document.createElement('div');
        outcomeDiv.className = `game-outcome ${gameOutcome.toLowerCase()}`;
        const outcomeStrong = document.createElement('strong');
        outcomeStrong.textContent = outcomeText;
        outcomeDiv.appendChild(outcomeStrong);

        resultArea.appendChild(outcomeDiv);

        if (gameOutcome !== 'EXPIRED') {
            const clientRollP = document.createElement('p');
            clientRollP.textContent = 'Your Roll: ';
            const clientRollStrong = document.createElement('strong');
            clientRollStrong.textContent = clientRoll;
            clientRollP.appendChild(clientRollStrong);

            const serverRollP = document.createElement('p');
            serverRollP.textContent = 'Server Roll: ';
            const serverRollStrong = document.createElement('strong');
            serverRollStrong.textContent = serverRoll;
            serverRollP.appendChild(serverRollStrong);

            resultArea.appendChild(clientRollP);
            resultArea.appendChild(serverRollP);
        }

        resultArea.classList.remove('hidden');
    }

    function showError(message) {
        errorArea.textContent = message;
        errorArea.classList.remove('hidden');
    }

    function updateGameHistory(clientRoll, serverRoll, gameOutcome) {
        const historyTable = document.querySelector('.history-table tbody');
        if (!historyTable) {
            createGameHistorySection(clientRoll, serverRoll, gameOutcome);
            return;
        }

        const newRow = createHistoryRow(clientRoll, serverRoll, gameOutcome);
        historyTable.insertBefore(newRow, historyTable.firstChild);

        const rows = historyTable.querySelectorAll('tr');
        if (rows.length > 5) {
            historyTable.removeChild(rows[rows.length - 1]);
        }
    }

    function createGameHistorySection(clientRoll, serverRoll, gameOutcome) {
        const gameContainer = document.querySelector('.game-container');
        const historySection = document.createElement('div');
        historySection.className = 'game-history';

        const heading = document.createElement('h1');
        heading.textContent = 'Recent Games';
        historySection.appendChild(heading);

        const table = document.createElement('table');
        table.className = 'history-table';

        const thead = document.createElement('thead');
        thead.innerHTML = `
            <tr>
                <th>Your Roll</th>
                <th>Server Roll</th>
                <th>Outcome</th>
                <th>Completed At</th>
            </tr>
        `;
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        tbody.appendChild(createHistoryRow(clientRoll, serverRoll, gameOutcome));
        table.appendChild(tbody);

        historySection.appendChild(table);
        gameContainer.appendChild(historySection);
    }

    function createHistoryRow(clientRoll, serverRoll, gameOutcome) {
        const row = document.createElement('tr');

        const clientRollCell = document.createElement('td');
        clientRollCell.textContent = clientRoll;
        row.appendChild(clientRollCell);

        const serverRollCell = document.createElement('td');
        serverRollCell.textContent = serverRoll;
        row.appendChild(serverRollCell);

        const outcomeCell = document.createElement('td');
        const outcomeBadge = document.createElement('span');
        outcomeBadge.className = `outcome-badge ${gameOutcome.toLowerCase().replace('_', '-')}`;
        outcomeBadge.textContent = gameOutcome.replace('_', ' ');
        outcomeCell.appendChild(outcomeBadge);
        row.appendChild(outcomeCell);

        const timestampCell = document.createElement('td');
        timestampCell.className = 'timestamp';
        timestampCell.textContent = formatTimestamp(new Date());
        row.appendChild(timestampCell);

        return row;
    }

    function formatTimestamps() {
        document.querySelectorAll('.timestamp').forEach(cell => {
            const timestamp = cell.dataset.timestamp;
            if (timestamp) {
                const date = new Date(timestamp);
                cell.textContent = formatTimestamp(date);
            }
        });
    }

    function formatTimestamp(date) {
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${day}/${month}, ${hours}:${minutes}`;
    }
});
