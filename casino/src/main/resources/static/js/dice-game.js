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
            const clientRoll = generateDiceRoll();

            const csrfElement = document.getElementById('csrf-token');
            const csrfToken = csrfElement.dataset.token;
            const csrfHeader = csrfElement.dataset.header;

            const initiateResponse = await fetch('/game', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ clientNonce })
            });

            if (!initiateResponse.ok) {
                window.location.href = '/error';
                return;
            }

            const { gameId, hashCommitment } = await initiateResponse.json();

            const guessResponse = await fetch(`/game/${gameId}/guess`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ clientRoll })
            });

            if (!guessResponse.ok) {
                window.location.href = '/error';
                return;
            }

            const { gameOutcome, serverRoll, serverNonce } = await guessResponse.json();

            const isValid = await verifyCommitment(serverRoll, serverNonce, clientNonce, hashCommitment);
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

    /**
     * Generates a fair dice roll (1-6) using rejection sampling to avoid modulo bias.
     *
     * Uint8Array gives values 0-255 (256 possible values). Since 256 % 6 = 4, using
     * simple modulo would make values 1-4 appear 43 times and values 5-6 appear 42 times.
     *
     * Solution: Reject values >= 252 (the largest multiple of 6 in 0-255), ensuring each
     * outcome (1-6) appears exactly 42 times. Rejection happens only 1.5% of the time.
     */
    function generateDiceRoll() {
        const array = new Uint8Array(1);
        let value;
        do {
            crypto.getRandomValues(array);
            value = array[0];
        } while (value >= 252);
        return (value % 6) + 1;
    }

    async function verifyCommitment(serverRoll, serverNonce, clientNonce, commitment) {
        const data = serverRoll + serverNonce + clientNonce;
        const encoder = new TextEncoder();
        const dataBuffer = encoder.encode(data);
        const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const computedHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        return computedHash === commitment;
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
