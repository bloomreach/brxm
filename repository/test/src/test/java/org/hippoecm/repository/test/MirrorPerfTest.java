package org.hippoecm.repository.test;

import org.hippoecm.testutils.history.HistoryWriter;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(HistoryWriter.class)
@Suite.SuiteClasses({
    org.hippoecm.repository.test.MirrorPerfTestCase.class
})
public class MirrorPerfTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
}
