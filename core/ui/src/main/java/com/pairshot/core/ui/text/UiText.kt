package com.pairshot.core.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed class UiText {
    data class Resource(
        @param:StringRes val resId: Int,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : UiText()

    data class Plural(
        @param:PluralsRes val resId: Int,
        val count: Int,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : UiText()

    data class Dynamic(
        val value: String,
    ) : UiText()

    @Composable
    fun asString(): String =
        when (this) {
            is Resource -> resolveCompose(resId, args)
            is Plural -> resolvePluralCompose(resId, count, args)
            is Dynamic -> value
        }

    fun asString(context: Context): String =
        when (this) {
            is Resource -> resolveContext(context, resId, args)
            is Plural -> resolvePluralContext(context, resId, count, args)
            is Dynamic -> value
        }
}

@Composable
private fun resolveCompose(
    resId: Int,
    args: ImmutableList<Any>,
): String =
    when {
        args.isEmpty() -> stringResource(resId)
        args.size == 1 -> stringResource(resId, args[0])
        args.size == 2 -> stringResource(resId, args[0], args[1])
        else -> stringResource(resId, args[0], args[1], args[2])
    }

@Composable
private fun resolvePluralCompose(
    resId: Int,
    count: Int,
    args: ImmutableList<Any>,
): String =
    when {
        args.isEmpty() -> pluralStringResource(resId, count)
        args.size == 1 -> pluralStringResource(resId, count, args[0])
        args.size == 2 -> pluralStringResource(resId, count, args[0], args[1])
        else -> pluralStringResource(resId, count, args[0], args[1], args[2])
    }

private fun resolveContext(
    context: Context,
    resId: Int,
    args: ImmutableList<Any>,
): String =
    when {
        args.isEmpty() -> context.getString(resId)
        args.size == 1 -> context.getString(resId, args[0])
        args.size == 2 -> context.getString(resId, args[0], args[1])
        else -> context.getString(resId, args[0], args[1], args[2])
    }

private fun resolvePluralContext(
    context: Context,
    resId: Int,
    count: Int,
    args: ImmutableList<Any>,
): String =
    when {
        args.isEmpty() -> context.resources.getQuantityString(resId, count)
        args.size == 1 -> context.resources.getQuantityString(resId, count, args[0])
        args.size == 2 -> context.resources.getQuantityString(resId, count, args[0], args[1])
        else -> context.resources.getQuantityString(resId, count, args[0], args[1], args[2])
    }
