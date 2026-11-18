package io.github.ccch1mneyyy.dsh_app.phone

import org.junit.Assert.*
import org.junit.Test

class SensitiveWordsRegressionTest {
    @Test fun englishActionsMatchWholeWordsAndPhrasesWithoutAResourceId() {
        for (label in listOf("Send", "DELETE", "Buy now", "Submit", "Pay", "Publish", "Sign out", "Log out", "Place order")) {
            assertNotNull(label, SensitiveWords.confirmHit(label, null))
        }
        assertEquals("发送", SensitiveWords.confirmHit("发送", null))
        assertEquals("send", SensitiveWords.confirmHit("", "example.app:id/btnSend"))
    }

    @Test fun ordinaryProseAndEmbeddedWordFragmentsDoNotBecomeActions() {
        for (label in listOf("Buyer guide", "Paymentology", "Resend history", "Calling hours",
            "A guide showing how to buy supplies from local shops")) {
            assertNull(label, SensitiveWords.confirmHit(label, null))
        }
        assertNotNull(SensitiveWords.confirmHit("Delete all saved messages permanently", null))
    }

    @Test fun sharedSecretHintsKeepOtpCompatibilityWithoutHotpotFalsePositives() {
        for (hint in listOf("Verification code", "Passcode", "OTP", "Enter otp code", "One-time code", "验证码", "校验码")) {
            assertNotNull(hint, SensitiveWords.secretHit(hint, null, null))
        }
        assertNotNull(SensitiveWords.secretHit(null, "Security passcode", null))
        assertNotNull(SensitiveWords.secretHit(null, null, "example.app:id/smsCode"))
        assertNotNull(SensitiveWords.secretHit(null, null, "example.app:id/otp"))
        for (hint in listOf("hotpot search", "Hotpot", "Search", "order number")) {
            assertNull(hint, SensitiveWords.secretHit(hint, null, null))
        }
        assertNull(SensitiveWords.secretHit(null, null, "example.app:id/hotpotSearch"))
    }

    @Test fun riskyActionCategoriesMatchWhileOrdinaryNavigationRemainsUnmarked() {
        for (label in listOf(
            "发表", "回复", "转发", "上传", "群发", "分享",
            "加好友", "添加好友", "添加到通讯录", "关注", "点赞", "红包",
            "保存", "移除", "清空", "退出账号",
            "结算", "充值", "提现", "退款", "订阅", "续费", "买入", "卖出", "撤单",
            "登录", "修改密码", "重置密码", "绑定", "解绑", "安装", "呼叫",
            "Like", "Reply", "Forward", "Upload", "Share", "Follow", "Add friend",
            "Save", "Remove", "Recharge", "Top up", "Withdraw", "Refund",
            "Subscribe", "Renew", "Login", "Sign in", "Bind", "Unbind", "Install",
            "Change password", "Reset password",
        )) {
            assertNotNull(label, SensitiveWords.confirmHit(label, null))
        }
        assertEquals("like", SensitiveWords.confirmHit("", "example.app:id/btnLike"))
        assertEquals("follow", SensitiveWords.confirmHit("", "example.app:id/followBtn"))
        assertEquals("upload", SensitiveWords.confirmHit("", "example.app:id/action_upload"))

        for (ordinary in listOf(
            "发消息", "搜索", "继续", "清除", "返回", "更多", "展开", "折叠",
            "加载更多", "下一页", "编辑", "草稿", "More like this", "Sounds like",
        )) {
            assertNull(ordinary, SensitiveWords.confirmHit(ordinary, null))
        }
    }
}
