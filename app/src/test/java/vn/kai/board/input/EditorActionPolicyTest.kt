package vn.kai.board.input

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorActionPolicyTest {
    @Test fun explicitSearchAndGoActionsWinOverEnterNewlineBehavior() {
        assertEquals(EditorInfo.IME_ACTION_SEARCH, EditorActionPolicy.resolve(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EditorInfo.IME_ACTION_GO, EditorActionPolicy.resolve(EditorInfo.IME_ACTION_GO))
    }

    @Test fun noneUnspecifiedAndNoEnterActionUseLineBreak() {
        assertNull(EditorActionPolicy.resolve(EditorInfo.IME_ACTION_NONE))
        assertNull(EditorActionPolicy.resolve(EditorInfo.IME_ACTION_UNSPECIFIED))
        assertNull(EditorActionPolicy.resolve(EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION))
    }
}
