package taptap.pub

sealed class Reaction<out T> {
    data class Success<out T>(val data: T) : Reaction<T>()
    data class Error(val exception: Exception) : Reaction<Nothing>()

    companion object {
        inline fun <T> of(f: () -> T): Reaction<T> = try {
            val result = f()
            if (result is Reaction<*>) {
                result as Reaction<T>
            } else {
                Success(result)
            }
        } catch (ex: Exception) {
            Error(ex)
        }
    }
}

inline fun <T> Reaction<T>.takeOrReturn(f: (Exception) -> Unit): T = when (this) {
    is Reaction.Success -> this.data
    is Reaction.Error -> {
        f(this.exception)
        throw IllegalStateException("You must write 'return' in the error lambda")
    }
}

inline fun <T, R> Reaction<T>.flatMap(f: (T) -> Reaction<R>) = try {
    when (this) {
        is Reaction.Success -> f(this.data)
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

inline fun <R, T> Reaction<T>.map(f: (T) -> R) = try {
    when (this) {
        is Reaction.Success -> Reaction.Success(f(this.data))
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

inline fun <T> Reaction<T>.errorMap(f: (Exception) -> Exception) = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> Reaction.Error(f(this.exception))
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

inline fun <T> Reaction<T>.flatHandle(f: (T?, Exception?) -> Unit) {
    when (this) {
        is Reaction.Success -> f(this.data, null)
        is Reaction.Error -> f(null, this.exception)
    }
}

inline fun <T> Reaction<T>.doOnComplete(f: () -> Unit) {
    f()
}

inline fun <T> Reaction<T>.handle(success: (T) -> Unit, error: (Exception) -> Unit) {
    when (this) {
        is Reaction.Success -> success(this.data)
        is Reaction.Error -> error(this.exception)
    }
}

inline fun <T, R> Reaction<T>.zip(success: (T) -> R, error: (Exception) -> R): R =
    when (this) {
        is Reaction.Success -> success(this.data)
        is Reaction.Error -> error(this.exception)
    }

inline fun <T> Reaction<T>.doOnSuccess(f: (T) -> Unit): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> {
            f(this.data)
            this
        }
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

inline fun <T> Reaction<T>.doOnError(f: (Exception) -> Unit): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> {
            f(this.exception)
            this
        }
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

inline fun <T> Reaction<T>.check(f: (T) -> Boolean, noinline lazyMessage: () -> String): Reaction<T> =
    try {
        when (this) {
            is Reaction.Success -> {
                check(f(this.data)) { lazyMessage }
                this
            }
            is Reaction.Error -> this
        }
    } catch (e: Exception) {
        Reaction.Error(e)
    }