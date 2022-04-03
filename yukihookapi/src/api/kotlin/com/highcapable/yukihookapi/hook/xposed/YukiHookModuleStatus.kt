/*
 * YukiHookAPI - An efficient Kotlin version of the Xposed Hook API.
 * Copyright (C) 2019-2022 HighCapable
 * https://github.com/fankes/YukiHookAPI
 *
 * MIT License
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
 * This file is Created by fankes on 2022/2/3.
 */
package com.highcapable.yukihookapi.hook.xposed

import android.app.Activity
import androidx.annotation.Keep
import com.highcapable.yukihookapi.annotation.DoNotUseAPI
import com.highcapable.yukihookapi.hook.factory.isModuleActive
import com.highcapable.yukihookapi.hook.factory.isTaiChiModuleActive
import com.highcapable.yukihookapi.hook.factory.isXposedModuleActive
import com.highcapable.yukihookapi.hook.log.yLoggerD
import com.highcapable.yukihookapi.hook.xposed.YukiHookModuleStatus.executorName
import com.highcapable.yukihookapi.hook.xposed.YukiHookModuleStatus.executorVersion
import de.robv.android.xposed.XposedBridge

/**
 * 这是一个 Xposed 模块 Hook 状态类
 *
 * 我们需要监听自己的模块是否被激活 - 可使用以下方法调用
 *
 * 在 [Activity] 中调用 [isModuleActive] 或 [isTaiChiModuleActive]
 *
 * 在任意地方调用 [isXposedModuleActive]
 *
 * 你还可以使用以下方法获取当前 Hook 框架的详细信息
 *
 * 调用 [executorName] 来获取当前 Hook 框架的名称
 *
 * 调用 [executorVersion] 来获取当前 Hook 框架的版本
 *
 * 详情请参考 [判断自身激活状态](https://github.com/fankes/YukiHookAPI/wiki#%E5%88%A4%E6%96%AD%E8%87%AA%E8%BA%AB%E6%BF%80%E6%B4%BB%E7%8A%B6%E6%80%81)
 */
object YukiHookModuleStatus {

    /** 定义 Jvm 方法名 */
    private const val IS_ACTIVE_METHOD_NAME = "__--"

    /** 定义 Jvm 方法名 */
    private const val GET_XPOSED_VERSION_METHOD_NAME = "--__"

    /** 定义 Jvm 方法名 */
    private const val GET_XPOSED_TAG_METHOD_NAME = "_-_-"

    /**
     * 获取当前 Hook 框架的名称
     *
     * 从 [XposedBridge] 获取 TAG
     * @return [String] 模块未激活会返回 unknown
     */
    val executorName
        get() = getXposedBridgeTag().replace(oldValue = "Bridge", newValue = "").replace(oldValue = "-", newValue = "").trim()

    /**
     * 获取当前 Hook 框架的版本
     *
     * 获取 [XposedBridge.getXposedVersion]
     * @return [Int] 模块未激活会返回 -1
     */
    val executorVersion get() = getXposedVersion()

    /**
     * 此方法经过 Hook 后返回 true 即模块已激活
     *
     * 请使用 [isModuleActive]、[isXposedModuleActive]、[isTaiChiModuleActive] 判断模块激活状态
     *
     * - ❗此方法为私有功能性 API - 你不应该手动调用此方法
     * @return [Boolean]
     */
    @Keep
    @DoNotUseAPI
    @JvmName(IS_ACTIVE_METHOD_NAME)
    internal fun isActive(): Boolean {
        yLoggerD(msg = IS_ACTIVE_METHOD_NAME, isDisableLog = true)
        return false
    }

    /**
     * 此方法经过 Hook 后返回 [XposedBridge.getXposedVersion]
     * @return [Int]
     */
    @Keep
    @JvmName(GET_XPOSED_VERSION_METHOD_NAME)
    private fun getXposedVersion(): Int {
        yLoggerD(msg = GET_XPOSED_VERSION_METHOD_NAME, isDisableLog = true)
        return -1
    }

    /**
     * 此方法经过 Hook 后返回 [XposedBridge] 的 TAG
     * @return [String]
     */
    @Keep
    @JvmName(GET_XPOSED_TAG_METHOD_NAME)
    private fun getXposedBridgeTag(): String {
        yLoggerD(msg = GET_XPOSED_TAG_METHOD_NAME, isDisableLog = true)
        return "unknown"
    }
}