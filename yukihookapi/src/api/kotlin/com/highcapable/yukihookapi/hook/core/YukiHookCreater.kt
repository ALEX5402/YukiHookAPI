/**
 * MIT License
 *
 * Copyright (C) 2022 HighCapable
 *
 * This file is part of YukiHookAPI.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This file is Created by fankes on 2022/2/2.
 */
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "EXPERIMENTAL_API_USAGE")

package com.highcapable.yukihookapi.hook.core

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.DoNotUseMethod
import com.highcapable.yukihookapi.hook.core.finder.ConstructorFinder
import com.highcapable.yukihookapi.hook.core.finder.FieldFinder
import com.highcapable.yukihookapi.hook.core.finder.MethodFinder
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.param.PackageParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Field
import java.lang.reflect.Member

/**
 * YukiHook 核心类实现方法
 *
 * 这是一个 API 对接类 - 实现原生对接 [XposedBridge]
 * @param packageParam 需要传入 [PackageParam] 实现方法调用
 * @param hookClass 要 Hook 的 [Class]
 */
class YukiHookCreater(private val packageParam: PackageParam, val hookClass: Class<*>) {

    /** 设置要 Hook 的方法、构造类 */
    private var hookMembers = HashMap<String, MemberHookCreater>()

    /**
     * 注入要 Hook 的方法、构造类
     * @param tag 可设置标签 - 在发生错误时方便进行调试
     * @param initiate 方法体
     */
    fun injectMember(tag: String = "Default", initiate: MemberHookCreater.() -> Unit) =
        MemberHookCreater(tag).apply(initiate).apply {
            hookMembers[toString()] = this
        }.build()

    /**
     * Hook 执行入口
     * - 此功能交由方法体自动完成 - 你不应该手动调用此方法
     * @throws IllegalStateException 如果必要参数没有被设置
     */
    @DoNotUseMethod
    fun hook() {
        if (hookMembers.isEmpty()) error("Hook Members is empty,hook aborted")
        hookMembers.forEach { (_, member) -> member.hook() }
    }

    /**
     * 智能全局方法、构造类查找类实现方法
     *
     * 处理需要 Hook 的方法
     * @param tag 当前设置的标签
     */
    inner class MemberHookCreater(var tag: String) {

        /** [beforeHook] 回调 */
        private var beforeHookCallback: (HookParam.() -> Unit)? = null

        /** [afterHook] 回调 */
        private var afterHookCallback: (HookParam.() -> Unit)? = null

        /** [replaceAny]、[replaceUnit]、[replaceTo] 等回调 */
        private var replaceHookCallback: (HookParam.() -> Any?)? = null

        /** Hook 过程中出现错误回调 */
        private var onConductFailureCallback: ((HookParam, Throwable) -> Unit)? = null

        /** Hook 开始时出现错误回调 */
        private var onHookingFailureCallback: ((Throwable) -> Unit)? = null

        /** 全部错误回调 */
        private var onAllFailureCallback: ((Throwable) -> Unit)? = null

        /** 是否为替换 Hook 模式 */
        private var isReplaceHookMode = false

        /** 标识是否已经设置了要 Hook 的 [member] */
        private var isHookMemberSetup = false

        /**
         * 手动指定要 Hook 的方法、构造类
         *
         * 你可以调用 [hookClass] 来手动查询要 Hook 的方法
         */
        var member: Member? = null

        /**
         * 查找需要 Hook 的方法
         *
         * 你只能使用一次 [method] 或 [constructor] 方法 - 否则结果会被替换
         * @param initiate 方法体
         * @return [MethodFinder.Result]
         */
        fun method(initiate: MethodFinder.() -> Unit): MethodFinder.Result {
            isHookMemberSetup = true
            return MethodFinder(hookInstance = this, hookClass).apply(initiate).build()
        }

        /**
         * 查找需要 Hook 的构造类
         *
         * 你只能使用一次 [method] 或 [constructor] 方法 - 否则结果会被替换
         * @param initiate 方法体
         * @return [ConstructorFinder.Result]
         */
        fun constructor(initiate: ConstructorFinder.() -> Unit): ConstructorFinder.Result {
            isHookMemberSetup = true
            return ConstructorFinder(hookInstance = this, hookClass).apply(initiate).build()
        }

        /**
         * 查找 [Field]
         * @param initiate 方法体
         * @return [FieldFinder.Result]
         */
        fun HookParam.field(initiate: FieldFinder.() -> Unit) =
            FieldFinder(hookInstance = this@MemberHookCreater, hookClass).apply(initiate).build()

        /**
         * 在方法执行完成前 Hook
         *
         * 不可与 [replaceAny]、[replaceUnit]、[replaceTo] 同时使用
         * @param initiate [HookParam] 方法体
         */
        fun beforeHook(initiate: HookParam.() -> Unit) {
            isReplaceHookMode = false
            beforeHookCallback = initiate
        }

        /**
         * 在方法执行完成后 Hook
         *
         * 不可与 [replaceAny]、[replaceUnit]、[replaceTo] 同时使用
         * @param initiate [HookParam] 方法体
         */
        fun afterHook(initiate: HookParam.() -> Unit) {
            isReplaceHookMode = false
            afterHookCallback = initiate
        }

        /**
         * 替换此方法内容 - 给出返回值
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         * @param initiate [HookParam] 方法体
         */
        fun replaceAny(initiate: HookParam.() -> Any?) {
            isReplaceHookMode = true
            replaceHookCallback = initiate
        }

        /**
         * 替换此方法内容 - 没有返回值 ([Unit])
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         * @param initiate [HookParam] 方法体
         */
        fun replaceUnit(initiate: HookParam.() -> Unit) {
            isReplaceHookMode = true
            replaceHookCallback = initiate
        }

        /**
         * 替换方法返回值
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         * @param any 要替换为的返回值对象
         */
        fun replaceTo(any: Any?) {
            isReplaceHookMode = true
            replaceHookCallback = { any }
        }

        /**
         * 替换方法返回值为 true
         *
         * 确保替换方法的返回对象为 [Boolean]
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         */
        fun replaceToTrue() {
            isReplaceHookMode = true
            replaceHookCallback = { true }
        }

        /**
         * 替换方法返回值为 false
         *
         * 确保替换方法的返回对象为 [Boolean]
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         */
        fun replaceToFalse() {
            isReplaceHookMode = true
            replaceHookCallback = { false }
        }

        /**
         * 拦截此方法
         *
         * 这将会禁止此方法执行并返回 null
         *
         * 不可与 [beforeHook]、[afterHook] 同时使用
         */
        fun intercept() {
            isReplaceHookMode = true
            replaceHookCallback = { null }
        }

        /**
         * Hook 创建入口
         * - 此功能交由方法体自动完成 - 你不应该手动调用此方法
         * @return [Result]
         */
        @DoNotUseMethod
        fun build() = Result()

        /**
         * Hook 执行入口
         * - 此功能交由方法体自动完成 - 你不应该手动调用此方法
         */
        @DoNotUseMethod
        fun hook() {
            if (member != null)
                member.also { member ->
                    runCatching {
                        if (isReplaceHookMode)
                            XposedBridge.hookMethod(member, object : XC_MethodReplacement() {
                                override fun replaceHookedMethod(baseParam: MethodHookParam?): Any? {
                                    if (baseParam == null) return null
                                    return HookParam(baseParam).let { param ->
                                        try {
                                            if (replaceHookCallback != null)
                                                onHookLogMsg(msg = "Replace Hook Member [${member}] done [$tag]")
                                            replaceHookCallback?.invoke(param)
                                        } catch (e: Throwable) {
                                            onConductFailureCallback?.invoke(param, e)
                                            onAllFailureCallback?.invoke(e)
                                            if (onConductFailureCallback == null && onAllFailureCallback == null)
                                                onHookFailureMsg(e)
                                            null
                                        }
                                    }
                                }
                            })
                        else
                            XposedBridge.hookMethod(member, object : XC_MethodHook() {
                                override fun beforeHookedMethod(baseParam: MethodHookParam?) {
                                    if (baseParam == null) return
                                    HookParam(baseParam).also { param ->
                                        runCatching {
                                            beforeHookCallback?.invoke(param)
                                            if (beforeHookCallback != null)
                                                onHookLogMsg(msg = "Before Hook Member [${member}] done [$tag]")
                                        }.onFailure {
                                            onConductFailureCallback?.invoke(param, it)
                                            onAllFailureCallback?.invoke(it)
                                            if (onConductFailureCallback == null && onAllFailureCallback == null)
                                                onHookFailureMsg(it)
                                        }
                                    }
                                }

                                override fun afterHookedMethod(baseParam: MethodHookParam?) {
                                    if (baseParam == null) return
                                    HookParam(baseParam).also { param ->
                                        runCatching {
                                            afterHookCallback?.invoke(param)
                                            if (afterHookCallback != null)
                                                onHookLogMsg(msg = "After Hook Member [${member}] done [$tag]")
                                        }.onFailure {
                                            onConductFailureCallback?.invoke(param, it)
                                            onAllFailureCallback?.invoke(it)
                                            if (onConductFailureCallback == null && onAllFailureCallback == null)
                                                onHookFailureMsg(it)
                                        }
                                    }
                                }
                            })
                    }.onFailure {
                        onHookingFailureCallback?.invoke(it)
                        onAllFailureCallback?.invoke(it)
                        if (onHookingFailureCallback == null && onAllFailureCallback == null) onHookFailureMsg(it)
                    }
                }
            else {
                onHookingFailureCallback?.invoke(Throwable())
                onAllFailureCallback?.invoke(Throwable())
                if (onHookingFailureCallback == null && onAllFailureCallback == null)
                    loggerE(
                        msg = if (isHookMemberSetup)
                            "Hooked Member with a finding error in Class [$hookClass] [$tag]"
                        else "Hooked Member cannot be non-null in Class [$hookClass] [$tag]"
                    )
            }
        }

        /**
         * Hook 过程中开启了 [YukiHookAPI.Configs.isDebug] 输出调试信息
         * @param msg 调试日志内容
         */
        internal fun onHookLogMsg(msg: String) {
            if (YukiHookAPI.Configs.isDebug) loggerI(msg = msg)
        }

        /**
         * Hook 失败但未设置 [onAllFailureCallback] 将默认输出失败信息
         * @param throwable 异常信息
         */
        private fun onHookFailureMsg(throwable: Throwable) =
            loggerE(msg = "Try to hook $hookClass[$member] got an Exception [$tag]", e = throwable)

        override fun toString() = "$member$tag#YukiHook"

        /**
         * 监听 Hook 结果实现类
         *
         * 可在这里处理失败事件监听
         */
        inner class Result {

            /**
             * 创建监听失败事件方法体
             * @param initiate 方法体
             * @return [Result] 可继续向下监听
             */
            fun failures(initiate: Result.() -> Unit) = apply(initiate)

            /**
             * 监听 Hook 进行过程中发生错误的回调方法
             * @param initiate 回调错误 - ([HookParam] 当前 Hook 实例,[Throwable] 异常)
             * @return [Result] 可继续向下监听
             */
            fun onConductFailure(initiate: (HookParam, Throwable) -> Unit): Result {
                onConductFailureCallback = initiate
                return this
            }

            /**
             * 忽略 Hook 进行过程中发生的错误
             * @return [Result] 可继续向下监听
             */
            fun ignoredConductFailure() = onConductFailure { _, _ -> }

            /**
             * 监听 Hook 开始时发生错误的回调方法
             * @param initiate 回调错误
             * @return [Result] 可继续向下监听
             */
            fun onHookingFailure(initiate: (Throwable) -> Unit): Result {
                onHookingFailureCallback = initiate
                return this
            }

            /**
             * 忽略 Hook 开始时发生的错误
             * @return [Result] 可继续向下监听
             */
            fun ignoredHookingFailure() = onHookingFailure {}

            /**
             * 监听全部 Hook 过程发生错误的回调方法
             * @param initiate 回调错误
             * @return [Result] 可继续向下监听
             */
            fun onAllFailure(initiate: (Throwable) -> Unit): Result {
                onAllFailureCallback = initiate
                return this
            }

            /**
             * 忽略全部 Hook 过程发生的错误
             * @return [Result] 可继续向下监听
             */
            fun ignoredAllFailure() = onAllFailure {}
        }
    }
}