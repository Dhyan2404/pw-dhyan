const http = require('http');
const fs = require('fs');

http.get('http://localhost:3300/player.html', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        fs.writeFileSync('d:\\Dhyan\\websites\\pwdhyan website\\player_dump.html', data);
        console.log('Saved player_dump.html, length:', data.length);
    });
}).on('error', (e) => {
    console.error('Error:', e.message);
});
