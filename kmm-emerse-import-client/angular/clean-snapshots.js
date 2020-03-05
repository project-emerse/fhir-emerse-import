// Removes all SNAPSHOT dependencies from package-lock.json to force updates.

const fs = require('fs');

process('package-lock.json');

function process(file) {
    console.log('Cleaning ' + file + '...');
    const inp = JSON.parse(fs.readFileSync(file, 'utf8'));
    const deps = inp.dependencies;
    let found = 0;

    for (const key of Object.keys(deps)) {
        const dep = deps[key];

        if (dep.version && dep.version.indexOf('-SNAPSHOT') > -1) {
            delete deps[key];
            found++;
        }
    }

    if (found) {
        console.log('Cleaned ' + found + ' snapshots.');
        fs.writeFileSync(file, JSON.stringify(inp, null, 2));
    } else {
        console.log('Found no snapshots.');
    }
}

