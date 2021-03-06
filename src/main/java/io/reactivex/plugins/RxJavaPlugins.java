/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.plugins;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.Experimental;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.schedulers.ComputationScheduler;
import io.reactivex.internal.schedulers.IoScheduler;
import io.reactivex.internal.schedulers.NewThreadScheduler;
import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
/**
 * Utility class to inject handlers to certain standard RxJava operations.
 */
public final class RxJavaPlugins {
    @Nullable
    static volatile Consumer<? super Throwable> errorHandler;

    @Nullable
    static volatile Function<Runnable, Runnable> onScheduleHandler;

    @Nullable
    static volatile Function<Callable<Scheduler>, Scheduler> onInitComputationHandler;

    @Nullable
    static volatile Function<Callable<Scheduler>, Scheduler> onInitSingleHandler;

    @Nullable
    static volatile Function<Callable<Scheduler>, Scheduler> onInitIoHandler;

    @Nullable
    static volatile Function<Callable<Scheduler>, Scheduler> onInitNewThreadHandler;

    @Nullable
    static volatile Function<Scheduler, Scheduler> onComputationHandler;

    @Nullable
    static volatile Function<Scheduler, Scheduler> onSingleHandler;

    @Nullable
    static volatile Function<Scheduler, Scheduler> onIoHandler;

    @Nullable
    static volatile Function<Scheduler, Scheduler> onNewThreadHandler;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<Flowable, Flowable> onFlowableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<ConnectableFlowable, ConnectableFlowable> onConnectableFlowableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<Observable, Observable> onObservableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<ConnectableObservable, ConnectableObservable> onConnectableObservableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<Maybe, Maybe> onMaybeAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<Single, Single> onSingleAssembly;

    static volatile Function<Completable, Completable> onCompletableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<ParallelFlowable, ParallelFlowable> onParallelAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<Maybe, MaybeObserver, MaybeObserver> onMaybeSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<Observable, Observer, Observer> onObservableSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<Single, SingleObserver, SingleObserver> onSingleSubscribe;

    @Nullable
    static volatile BiFunction<Completable, CompletableObserver, CompletableObserver> onCompletableSubscribe;

    @Nullable
    static volatile BooleanSupplier onBeforeBlocking;

    /** Prevents changing the plugins. */
    static volatile boolean lockdown;

    /**
     * If true, attempting to run a blockingX operation on a (by default)
     * computation or single scheduler will throw an IllegalStateException.
     */
    static volatile boolean failNonBlockingScheduler;

    /**
     * Prevents changing the plugins from then on.
     * <p>This allows container-like environments to prevent clients
     * messing with plugins.
     */
    public static void lockdown() {
        lockdown = true;
    }

    /**
     * Returns true if the plugins were locked down.
     * @return true if the plugins were locked down
     */
    public static boolean isLockdown() {
        return lockdown;
    }

    /**
     * Enables or disables the blockingX operators to fail
     * with an IllegalStateException on a non-blocking
     * scheduler such as computation or single.
     * @param enable enable or disable the feature
     * @since 2.0.5 - experimental
     */
    @Experimental
    public static void setFailOnNonBlockingScheduler(boolean enable) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        failNonBlockingScheduler = enable;
    }

    /**
     * Returns true if the blockingX operators fail
     * with an IllegalStateException on a non-blocking scheduler
     * such as computation or single.
     * @return true if the blockingX operators fail on a non-blocking scheduler
     * @since 2.0.5 - experimental
     */
    @Experimental
    public static boolean isFailOnNonBlockingScheduler() {
        return failNonBlockingScheduler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Scheduler, Scheduler> getComputationSchedulerHandler() {
        return onComputationHandler;
    }

    /**
     * Returns the a hook consumer.
     * @return the hook consumer, may be null
     */
    @Nullable
    public static Consumer<? super Throwable> getErrorHandler() {
        return errorHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Callable<Scheduler>, Scheduler> getInitComputationSchedulerHandler() {
        return onInitComputationHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Callable<Scheduler>, Scheduler> getInitIoSchedulerHandler() {
        return onInitIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Callable<Scheduler>, Scheduler> getInitNewThreadSchedulerHandler() {
        return onInitNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Callable<Scheduler>, Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Scheduler, Scheduler> getIoSchedulerHandler() {
        return onIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Scheduler, Scheduler> getNewThreadSchedulerHandler() {
        return onNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Runnable, Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Scheduler, Scheduler> getSingleSchedulerHandler() {
        return onSingleHandler;
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initComputationScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<Callable<Scheduler>, Scheduler> f = onInitComputationHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler); // JIT will skip this
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initIoScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<Callable<Scheduler>, Scheduler> f = onInitIoHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initNewThreadScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<Callable<Scheduler>, Scheduler> f = onInitNewThreadHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initSingleScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<Callable<Scheduler>, Scheduler> f = onInitSingleHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onComputationScheduler(@NonNull Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Called when an undeliverable error occurs.
     * @param error the error to report
     */
    public static void onError(@NonNull Throwable error) {
        Consumer<? super Throwable> f = errorHandler;

        if (error == null) {
            error = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        }

        if (f != null) {
            try {
                f.accept(error);
                return;
            } catch (Throwable e) {
                // Exceptions.throwIfFatal(e); TODO decide
                e.printStackTrace(); // NOPMD
                uncaught(e);
            }
        }

        error.printStackTrace(); // NOPMD
        uncaught(error);
    }

    static void uncaught(@NonNull Throwable error) {
        Thread currentThread = Thread.currentThread();
        UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
        handler.uncaughtException(currentThread, error);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onIoScheduler(@NonNull Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onIoHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onNewThreadScheduler(@NonNull Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onNewThreadHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Called when a task is scheduled.
     * @param run the runnable instance
     * @return the replacement runnable
     */
    @NonNull
    public static Runnable onSchedule(@NonNull Runnable run) {
        Function<Runnable, Runnable> f = onScheduleHandler;
        if (f == null) {
            return run;
        }
        return apply(f, run);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onSingleScheduler(@NonNull Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Removes all handlers and resets to default behavior.
     */
    public static void reset() {
        setErrorHandler(null);
        setScheduleHandler(null);

        setComputationSchedulerHandler(null);
        setInitComputationSchedulerHandler(null);

        setIoSchedulerHandler(null);
        setInitIoSchedulerHandler(null);

        setSingleSchedulerHandler(null);
        setInitSingleSchedulerHandler(null);

        setNewThreadSchedulerHandler(null);
        setInitNewThreadSchedulerHandler(null);

        setOnFlowableAssembly(null);
        setOnFlowableSubscribe(null);

        setOnObservableAssembly(null);
        setOnObservableSubscribe(null);

        setOnSingleAssembly(null);
        setOnSingleSubscribe(null);

        setOnCompletableAssembly(null);
        setOnCompletableSubscribe(null);

        setOnConnectableFlowableAssembly(null);
        setOnConnectableObservableAssembly(null);

        setOnMaybeAssembly(null);
        setOnMaybeSubscribe(null);

        setOnParallelAssembly(null);

        setFailOnNonBlockingScheduler(false);
        setOnBeforeBlocking(null);
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setComputationSchedulerHandler(@Nullable Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setErrorHandler(@Nullable Consumer<? super Throwable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        errorHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitComputationSchedulerHandler(@Nullable Function<Callable<Scheduler>, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitIoSchedulerHandler(@Nullable Function<Callable<Scheduler>, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitNewThreadSchedulerHandler(@Nullable Function<Callable<Scheduler>, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitSingleSchedulerHandler(@Nullable Function<Callable<Scheduler>, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitSingleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setIoSchedulerHandler(@Nullable Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setNewThreadSchedulerHandler(@Nullable Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setScheduleHandler(@Nullable Function<Runnable, Runnable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onScheduleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setSingleSchedulerHandler(@Nullable Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onSingleHandler = handler;
    }

    /**
     * Revokes the lockdown, only for testing purposes.
     */
    /* test. */static void unlock() {
        lockdown = false;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<Completable, Completable> getOnCompletableAssembly() {
        return onCompletableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static BiFunction<Completable, CompletableObserver, CompletableObserver> getOnCompletableSubscribe() {
        return onCompletableSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<Flowable, Flowable> getOnFlowableAssembly() {
        return onFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<ConnectableFlowable, ConnectableFlowable> getOnConnectableFlowableAssembly() {
        return onConnectableFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<Flowable, Subscriber, Subscriber> getOnFlowableSubscribe() {
        return onFlowableSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<Maybe, MaybeObserver, MaybeObserver> getOnMaybeSubscribe() {
        return onMaybeSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<Maybe, Maybe> getOnMaybeAssembly() {
        return onMaybeAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<Single, Single> getOnSingleAssembly() {
        return onSingleAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<Single, SingleObserver, SingleObserver> getOnSingleSubscribe() {
        return onSingleSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<Observable, Observable> getOnObservableAssembly() {
        return onObservableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<ConnectableObservable, ConnectableObservable> getOnConnectableObservableAssembly() {
        return onConnectableObservableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<Observable, Observer, Observer> getOnObservableSubscribe() {
        return onObservableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onCompletableAssembly the hook function to set, null allowed
     */
    public static void setOnCompletableAssembly(@Nullable Function<Completable, Completable> onCompletableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableAssembly = onCompletableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onCompletableSubscribe the hook function to set, null allowed
     */
    public static void setOnCompletableSubscribe(
            @Nullable BiFunction<Completable, CompletableObserver, CompletableObserver> onCompletableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableSubscribe = onCompletableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onFlowableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableAssembly(@Nullable Function<Flowable, Flowable> onFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableAssembly = onFlowableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onMaybeAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnMaybeAssembly(@Nullable Function<Maybe, Maybe> onMaybeAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onMaybeAssembly = onMaybeAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onConnectableFlowableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnConnectableFlowableAssembly(@Nullable Function<ConnectableFlowable, ConnectableFlowable> onConnectableFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onConnectableFlowableAssembly = onConnectableFlowableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onFlowableSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableSubscribe(@Nullable BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableSubscribe = onFlowableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onMaybeSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnMaybeSubscribe(@Nullable BiFunction<Maybe, MaybeObserver, MaybeObserver> onMaybeSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onMaybeSubscribe = onMaybeSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onObservableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableAssembly(@Nullable Function<Observable, Observable> onObservableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableAssembly = onObservableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onConnectableObservableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnConnectableObservableAssembly(@Nullable Function<ConnectableObservable, ConnectableObservable> onConnectableObservableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onConnectableObservableAssembly = onConnectableObservableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onObservableSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableSubscribe(
            @Nullable BiFunction<Observable, Observer, Observer> onObservableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableSubscribe = onObservableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onSingleAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnSingleAssembly(@Nullable Function<Single, Single> onSingleAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleAssembly = onSingleAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onSingleSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnSingleSubscribe(@Nullable BiFunction<Single, SingleObserver, SingleObserver> onSingleSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleSubscribe = onSingleSubscribe;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param subscriber the subscriber
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Subscriber<? super T> onSubscribe(@NonNull Flowable<T> source, @NonNull Subscriber<? super T> subscriber) {
        BiFunction<Flowable, Subscriber, Subscriber> f = onFlowableSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Observer<? super T> onSubscribe(@NonNull Observable<T> source, @NonNull Observer<? super T> observer) {
        BiFunction<Observable, Observer, Observer> f = onObservableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> SingleObserver<? super T> onSubscribe(@NonNull Single<T> source, @NonNull SingleObserver<? super T> observer) {
        BiFunction<Single, SingleObserver, SingleObserver> f = onSingleSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @NonNull
    public static CompletableObserver onSubscribe(@NonNull Completable source, @NonNull CompletableObserver observer) {
        BiFunction<Completable, CompletableObserver, CompletableObserver> f = onCompletableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param subscriber the subscriber
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> MaybeObserver<? super T> onSubscribe(@NonNull Maybe<T> source, @NonNull MaybeObserver<? super T> subscriber) {
        BiFunction<Maybe, MaybeObserver, MaybeObserver> f = onMaybeSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Maybe<T> onAssembly(@NonNull Maybe<T> source) {
        Function<Maybe, Maybe> f = onMaybeAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Flowable<T> onAssembly(@NonNull Flowable<T> source) {
        Function<Flowable, Flowable> f = onFlowableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ConnectableFlowable<T> onAssembly(@NonNull ConnectableFlowable<T> source) {
        Function<ConnectableFlowable, ConnectableFlowable> f = onConnectableFlowableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
        Function<Observable, Observable> f = onObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ConnectableObservable<T> onAssembly(@NonNull ConnectableObservable<T> source) {
        Function<ConnectableObservable, ConnectableObservable> f = onConnectableObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Single<T> onAssembly(@NonNull Single<T> source) {
        Function<Single, Single> f = onSingleAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Completable onAssembly(@NonNull Completable source) {
        Function<Completable, Completable> f = onCompletableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     * @since 2.0.6 - experimental
     */
    @Experimental
    @SuppressWarnings("rawtypes")
    public static void setOnParallelAssembly(@Nullable Function<ParallelFlowable, ParallelFlowable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onParallelAssembly = handler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     * @since 2.0.6 - experimental
     */
    @Experimental
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<ParallelFlowable, ParallelFlowable> getOnParallelAssembly() {
        return onParallelAssembly;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type of the source
     * @param source the hook's input value
     * @return the value returned by the hook
     * @since 2.0.6 - experimental
     */
    @Experimental
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ParallelFlowable<T> onAssembly(@NonNull ParallelFlowable<T> source) {
        Function<ParallelFlowable, ParallelFlowable> f = onParallelAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Called before an operator attempts a blocking operation
     * such as awaiting a condition or signal
     * and should return true to indicate the operator
     * should not block but throw an IllegalArgumentException.
     * @return true if the blocking should be prevented
     * @see #setFailOnNonBlockingScheduler(boolean)
     * @since 2.0.5 - experimental
     */
    @Experimental
    public static boolean onBeforeBlocking() {
        BooleanSupplier f = onBeforeBlocking;
        if (f != null) {
            try {
                return f.getAsBoolean();
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return false;
    }

    /**
     * Set the handler that is called when an operator attemts a blocking
     * await; the handler should return true to prevent the blocking
     * and to signal an IllegalStateException instead.
     * @param handler the handler to set, null resets to the default handler
     * that always returns false
     * @see #onBeforeBlocking()
     * @since 2.0.5 - experimental
     */
    @Experimental
    public static void setOnBeforeBlocking(@Nullable BooleanSupplier handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onBeforeBlocking = handler;
    }

    /**
     * Returns the current blocking handler or null if no custom handler
     * is set.
     * @return the current blocking handler or null if not specified
     * @since 2.0.5 - experimental
     */
    @Experimental
    @Nullable
    public static BooleanSupplier getOnBeforeBlocking() {
        return onBeforeBlocking;
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#computation()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.0.5 - experimental
     */
    @Experimental
    @NonNull
    public static Scheduler createComputationScheduler(@NonNull ThreadFactory threadFactory) {
        return new ComputationScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#io()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.0.5 - experimental
     */
    @Experimental
    @NonNull
    public static Scheduler createIoScheduler(@NonNull ThreadFactory threadFactory) {
        return new IoScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#newThread()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.0.5 - experimental
     */
    @Experimental
    @NonNull
    public static Scheduler createNewThreadScheduler(@NonNull ThreadFactory threadFactory) {
        return new NewThreadScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#single()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.0.5 - experimental
     */
    @Experimental
    @NonNull
    public static Scheduler createSingleScheduler(@NonNull ThreadFactory threadFactory) {
        return new SingleScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the parameter value to the function
     * @return the result of the function call
     */
    @NonNull
    static <T, R> R apply(@NonNull Function<T, R> f, @NonNull T t) {
        try {
            return f.apply(t);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the first input type
     * @param <U> the second input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the first parameter value to the function
     * @param u the second parameter value to the function
     * @return the result of the function call
     */
    @NonNull
    static <T, U, R> R apply(@NonNull BiFunction<T, U, R> f, @NonNull T t, @NonNull U u) {
        try {
            return f.apply(t, u);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the Scheduler creation callable in try-catch and propagates thrown
     * checked exceptions as RuntimeException and enforces that result is not null.
     * @param s the {@link Callable} which returns a {@link Scheduler}, not null (not verified). Cannot return null
     * @return the result of the callable call, not null
     * @throws NullPointerException if the callable parameter returns null
     */
    @NonNull
    static Scheduler callRequireNonNull(@NonNull Callable<Scheduler> s) {
        try {
            return ObjectHelper.requireNonNull(s.call(), "Scheduler Callable result can't be null");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the Scheduler creation function in try-catch and propagates thrown
     * checked exceptions as RuntimeException and enforces that result is not null.
     * @param f the function to call, not null (not verified). Cannot return null
     * @param s the parameter value to the function
     * @return the result of the function call, not null
     * @throws NullPointerException if the function parameter returns null
     */
    @NonNull
    static Scheduler applyRequireNonNull(@NonNull Function<Callable<Scheduler>, Scheduler> f, Callable<Scheduler> s) {
        return ObjectHelper.requireNonNull(apply(f, s), "Scheduler Callable result can't be null");
    }

    /** Helper class, no instances. */
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
