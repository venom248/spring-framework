/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于以编程方式注册 bean 类的便捷适配器。
 * 这是 {@link ClassPathBeanDefinitionScanner} 的 替代方案，应用相同的注释解析，但仅适用于明确注册的类。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private ConditionEvaluator conditionEvaluator;


	/**
	 * 创建一个指定 BeanDefinitionRegistry 的 {@code AnnotatedBeanDefinitionReader}
	 * <p>如果 registry 是 {@link EnvironmentCapable}，例如是 {@code ApplicationContext}，
	 * 则将继承 {@link Environment}，否则将创建并使用一个新的 {@link StandardEnvironment}。
	 * @param registry 要加载 bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * 创建一个指定 BeanDefinitionRegistry 的 {@code AnnotatedBeanDefinitionReader}，使用给定的 {@link Environment}。
	 * @param registry 要加载 bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式
	 * @param environment 用于评估 bean 定义配置文件的 {@code Environment}
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * Get the BeanDefinitionRegistry that this reader operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the {@code Environment} to use when evaluating whether
	 * {@link Conditional @Conditional}-annotated component classes should be registered.
	 * <p>The default is a {@link StandardEnvironment}.
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * Set the {@code BeanNameGenerator} to use for detected bean classes.
	 * <p>The default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * Set the {@code ScopeMetadataResolver} to use for registered component classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 注册一个或多个需要处理的 component
	 * <p>对 {@code register} 的调用是幂等的；多次添加相同的 component 类不会产生额外效果。
	 * @param componentClasses 一个或多个 component 类，例如 {@link Configuration @Configuration} 类
	 */
	public void register(Class<?>... componentClasses) {
		for (Class<?> componentClass : componentClasses) {
			registerBean(componentClass);
		}
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 */
	public void registerBean(Class<?> beanClass) {
		doRegisterBean(beanClass, null, null, null, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 * @param name bean 的显式名称（或 {@code null} 以生成默认 bean 名称）
	 */
	public void registerBean(Class<?> beanClass, @Nullable String name) {
		doRegisterBean(beanClass, name, null, null, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 * @param qualifiers 要考虑的特定限定符注释（如果有的话），除了 bean 类级别的限定符
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(beanClass, null, qualifiers, null, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 * @param name bean 的显式名称（或 {@code null} 以生成默认 bean 名称）
	 * @param qualifiers 要考虑的特定限定符注释（如果有的话），除了 bean 类级别的限定符
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, @Nullable String name,
			Class<? extends Annotation>... qualifiers) {

		doRegisterBean(beanClass, name, qualifiers, null, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据, 使用给定的供应商获取新实例（可能声明为 lambda 表达式或方法引用）。
	 * @param beanClass bean 的类
	 * @param supplier 创建 bean 实例的回调（可能为 {@code null}）
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, null, null, supplier, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据, 使用给定的供应商获取新实例（可能声明为 lambda 表达式或方法引用）。
	 * @param beanClass bean 的类
	 * @param name bean 的显式名称（或 {@code null} 以生成默认 bean 名称）
	 * @param supplier 创建 bean 实例的回调（可能为 {@code null}）
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, name, null, supplier, null);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 * @param name bean 的显式名称（或 {@code null} 以生成默认 bean 名称）
	 * @param supplier 创建 bean 实例的回调（可能为 {@code null}）
	 * @param customizers 一个或多个用于自定义工厂的 {@link BeanDefinition} 的回调，例如设置懒加载或主要标志
	 * @since 5.2
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
			BeanDefinitionCustomizer... customizers) {

		doRegisterBean(beanClass, name, null, supplier, customizers);
	}

	/**
	 * 从给定的 bean 类中注册成 bean，从类声明的注释中获取其元数据。
	 * @param beanClass bean 的类
	 * @param name bean 的显式名称（或 {@code null} 以生成默认 bean 名称）
	 * @param qualifiers 要考虑的特定限定符注释（如果有的话），除了 bean 类级别的限定符
	 * @param supplier 创建 bean 实例的回调（可能为 {@code null}）
	 * @param customizers 一个或多个用于自定义工厂的 {@link BeanDefinition} 的回调，例如设置懒加载或主要标志
	 * @since 5.0
	 */
	private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
			@Nullable BeanDefinitionCustomizer[] customizers) {

		// 创建一个 AnnotatedGenericBeanDefinition(BeanDefinition) 对象，基于传入的 beanClass
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);

		// 判断这个 bean 是否应被跳过（基于条件注解）。如果应被跳过，直接返回。
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		// 设置 CANDIDATE_ATTRIBUTE 属性为 true，表示这个类是一个候选的配置类。
		abd.setAttribute(ConfigurationClassUtils.CANDIDATE_ATTRIBUTE, Boolean.TRUE);
		// 设置实例 supplier
		abd.setInstanceSupplier(supplier);
		// 解析并设置 bean 的作用域。
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		// 生成 bean 的名称。如果提供了 name，则使用提供的名称；否则，自动生成一个名称。
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

		// 处理常见的注解（如 @Lazy, @Primary 等）。
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);

		// 如果提供了 qualifiers，处理这些限定符注解。
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true); // 如果是 @Primary，设置为 primary。
				}
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true); // 如果是 @Lazy，设置为 lazy 初始化。
				}
				else {
					// 其他情况，添加限定符。
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}

		// 如果提供了 customizers，调用它们来定制 BeanDefinition。
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				customizer.customize(abd);
			}
		}

		// 创建一个 BeanDefinitionHolder 对象，持有 BeanDefinition 和 bean 的名称。
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		// 应用作用域代理模式（如果需要），并返回可能被代理过的 BeanDefinitionHolder
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		// 将 BeanDefinitionHolder 注册到 registry 中
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * 如果可能的话，从给定的 registry 中获取 Environment，否则返回一个新的 StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable environmentCapable) {
			return environmentCapable.getEnvironment();
		}
		return new StandardEnvironment();
	}

}
