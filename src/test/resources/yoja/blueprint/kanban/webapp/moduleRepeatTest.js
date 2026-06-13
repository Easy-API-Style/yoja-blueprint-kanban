'use strict'

/**
 * Repeat demo for {@code repeatTestModuleUntil()}: the default export below is
 * re-invoked on every poll until it calls {@code resolve()} or the deadline
 * elapses. On each run it checks whether the login form has been rendered;
 * once present it resolves, otherwise the polling loop runs again.
 *
 * @param {Array<*>} args        extra arguments forwarded from the Java side
 * @param {function():void} resolve callback to fire once the condition is met
 * @param {function():void} repeat callback to request another poll iteration
 */
export default function(args, resolve, repeat) {
    if (yojaWeb.firstTag('form')) {
        resolve()
    }
}
