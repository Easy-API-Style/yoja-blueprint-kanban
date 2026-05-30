'use strict'

/**
 * Synchronous jsUnit demo — invoked from Java via {@code testJsUnit()}.
 *
 * Each named export below is run as its own test step by the Java
 * orchestration; a thrown error fails just that step. {@code ywAssert} is
 * loaded into the page from the Java side via {@code .loadYwAssert()} before
 * this module is imported, so it is globally available here.
 */

/**
 * Asserts the document title matches the expected app name.
 */
export function titleIsTaskManager() {
    ywAssert.assertEquals('Yoja Task Manager',
                          document.title,
                          'document title')
}

/**
 * Asserts a login form is present in the DOM.
 */
export function loginFormIsPresent() {
    ywAssert.assertNotNull(yojaWeb.firstTag('form'),
                           'login form should exist on the page')
}
