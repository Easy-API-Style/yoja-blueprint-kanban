'use strict'

/**
 * Sync demo for {@code testModule()}: the default export below is invoked
 * once, synchronously. Throw to fail; otherwise the step is reported as
 * passing. {@code ywAssert} is loaded into the page by the Java side via
 * {@code .loadYwAssert()} before this module is imported, so it is globally
 * available here.
 */
export default function() {
    ywAssert.assertEquals('Yoja Task Manager',
                          document.title,
                          'document title')
    ywAssert.assertNotNull(yojaWeb.firstTag('form'),
                           'login form should be present on the page')
}
