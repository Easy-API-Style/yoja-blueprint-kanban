'use strict'

/**
 * Async demo for {@code testAsyncModule()}: the default export below
 * receives {@code (args, resolve, reject)} and MUST end by calling either
 * {@code resolve()} (success) or {@code reject(message)} (failure). The
 * {@code args} parameter carries any extra {@code Object...} values passed
 * from the Java side — unused in this scenario.
 *
 * @param {Array<*>} args        extra arguments forwarded from the Java side
 * @param {function():void} resolve callback to fire on success
 * @param {function(string):void} reject callback to fire on failure
 */
export default function(args, resolve, reject) {
    fetch('/api/login', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ username: 'demo', password: 'demo' })
    })
    .then(function(res) {
        ywAssert.assertEquals(200, res.status,
                              'POST /api/login should respond 200 with valid credentials')
        return res.json()
    })
    .then(function(body) {
        ywAssert.assertEquals('demo', body.user,
                              'response body should echo the logged-in user')
        resolve()
    })
    .catch(function(e) {
        reject(e.message)
    })
}
