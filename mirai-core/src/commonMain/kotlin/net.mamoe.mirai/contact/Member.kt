/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("unused")

package net.mamoe.mirai.contact

import net.mamoe.mirai.Bot
import net.mamoe.mirai.JavaHappyAPI
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiInternalAPI
import net.mamoe.mirai.utils.WeakRefProperty
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * 群成员.
 */ // 不要删除多平台结构, kotlin bug
@Suppress("INAPPLICABLE_JVM_NAME")
@UseExperimental(MiraiInternalAPI::class, JavaHappyAPI::class)
expect abstract class Member() : MemberJavaHappyAPI {
    /**
     * 所在的群.
     */
    @WeakRefProperty
    abstract val group: Group

    /**
     * 成员的权限, 动态更新.
     *
     * @see MemberPermissionChangeEvent 权限变更事件. 由群主或机器人的操作触发.
     */
    abstract val permission: MemberPermission

    /**
     * 群名片. 可能为空.
     *
     * 管理员和群主都可修改任何人（包括群主）的群名片.
     *
     * 在修改时将会异步上传至服务器.
     *
     * @see [nameCardOrNick] 获取非空群名片或昵称
     *
     * @see MemberCardChangeEvent 群名片被管理员, 自己或 [Bot] 改动事件. 修改时也会触发此事件.
     * @throws PermissionDeniedException 无权限修改时
     */
    abstract var nameCard: String

    /**
     * 群头衔.
     *
     * 仅群主可以修改群头衔.
     *
     * 在修改时将会异步上传至服务器.
     *
     * @see MemberSpecialTitleChangeEvent 群名片被管理员, 自己或 [Bot] 改动事件. 修改时也会触发此事件.
     * @throws PermissionDeniedException 无权限修改时
     */
    abstract var specialTitle: String

    /**
     * 被禁言剩余时长. 单位为秒.
     *
     * @see isMuted 判断改成员是否处于禁言状态
     * @see mute 设置禁言
     * @see unmute 取消禁言
     */
    abstract val muteTimeRemaining: Int

    /**
     * 禁言.
     *
     * QQ 中最小操作和显示的时间都是一分钟.
     * 机器人可以实现精确到秒, 会被客户端显示为 1 分钟但不影响实际禁言时间.
     *
     * 管理员可禁言成员, 群主可禁言管理员和群员.
     *
     * @param durationSeconds 持续时间. 精确到秒. 范围区间表示为 `(0s, 30days]`. 超过范围则会抛出异常.
     * @return 机器人无权限时返回 `false`
     *
     * @see Int.minutesToSeconds
     * @see Int.hoursToSeconds
     * @see Int.daysToSeconds
     *
     * @see MemberMuteEvent 成员被禁言事件
     * @throws PermissionDeniedException 无权限修改时
     */
    @JvmName("muteSuspend")
    @JvmSynthetic
    abstract suspend fun mute(durationSeconds: Int)

    /**
     * 解除禁言.
     *
     * 管理员可解除成员的禁言, 群主可解除管理员和群员的禁言.
     *
     * @see MemberUnmuteEvent 成员被取消禁言事件.
     * @throws PermissionDeniedException 无权限修改时
     */
    @JvmName("unmuteSuspend")
    @JvmSynthetic
    abstract suspend fun unmute()

    /**
     * 踢出该成员.
     *
     * 管理员可踢出成员, 群主可踢出管理员和群员.
     *
     * @see MemberLeaveEvent.Kick 成员被踢出事件.
     * @throws PermissionDeniedException 无权限修改时
     */
    @JvmName("kickSuspend")
    @JvmSynthetic
    abstract suspend fun kick(message: String = "")

    /**
     * 当且仅当 `[other] is [Member] && [other].id == this.id && [other].group == this.group` 时为 true
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * @return `bot.hashCode() * 31 + id.hashCode()`
     */
    abstract override fun hashCode(): Int
}

/**
 * 获取非空群名片或昵称.
 *
 * 若 [群名片][Member.nameCard] 不为空则返回群名片, 为空则返回 [QQ.nick]
 */
val Member.nameCardOrNick: String get() = this.nameCard.takeIf { it.isNotEmpty() } ?: this.nick

/**
 * 判断改成员是否处于禁言状态.
 */
fun Member.isMuted(): Boolean {
    return muteTimeRemaining != 0 && muteTimeRemaining != 0xFFFFFFFF.toInt()
}

@ExperimentalTime
suspend inline fun Member.mute(duration: Duration) {
    require(duration.inDays <= 30) { "duration must be at most 1 month" }
    require(duration.inSeconds > 0) { "duration must be greater than 0 second" }
    this.mute(duration.inSeconds.toInt())
}

suspend inline fun Member.mute(durationSeconds: Long) = this.mute(durationSeconds.toInt())