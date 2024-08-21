/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 独立的应用上下文，接受 <em>组件类</em> 作为输入 —— 特别是使用 {@link Configuration @Configuration} 注解的类，
 * 但也包括普通的 {@link org.springframework.stereotype.Component @Component} 类型
 * 和使用 {@code jakarta.inject} 注解的 JSR-330 兼容类。
 *
 * <p>允许使用 {@link #register(Class...)} 一个一个地注册类，也可以使用 {@link #scan(String...)} 进行类路径扫描。
 *
 * <p>在多个 {@code @Configuration} 类的情况下，后面类中定义的 {@link Bean @Bean} 方法将覆盖前面类中定义的方法。
 * 这可以通过一个额外的 {@code @Configuration} 类来有意地覆盖某些bean定义。
 *
 * <p>有关使用示例，请参见 {@link Configuration @Configuration} 的javadoc。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see #register 用于注册一个或多个组件类
 * @see #scan 用于扫描类路径
 * @see AnnotatedBeanDefinitionReader 用于读取注解并注册Bean
 * @see ClassPathBeanDefinitionScanner 用于扫描类路径并注册Bean
 * @see org.springframework.context.support.GenericXmlApplicationContext 用于从 XML 文件中加载上下文
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	private final AnnotatedBeanDefinitionReader reader;

	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * 创建一个需要填充的新 AnnotationConfigApplicationContext
	 * 通过 {@link #register} 调用，然后手动 {@linkplain #refresh 刷新}。
	 */
	public AnnotationConfigApplicationContext() {
		// 启动一个性能监控步骤，标记为: spring.context.annotated-bean-reader.create
		// `StartupStep` 是 Spring 中用于性能监控的一个类，这一步开始跟踪 AnnotatedBeanDefinitionReader 的创建过程
		StartupStep createAnnotatedBeanDefReader = getApplicationStartup().start("spring.context.annotated-bean-reader.create");
		// 创建一个 AnnotatedBeanDefinitionReader 实例
		// AnnotatedBeanDefinitionReader 是一个用于读取注解并注入到应用上下文中的 BeanDefinition 的类
		// 这里将自身作为 BeanDefinitionRegistry 传入
		this.reader = new AnnotatedBeanDefinitionReader(this);
		// 结束性能监控步骤
		createAnnotatedBeanDefReader.end();
		// 创建一个`ClassPathBeanDefinitionScanner`实例
		// `ClassPathBeanDefinitionScanner` 是一个用于扫描类路径并将扫描到的类注册到应用上下文中的类
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，使用给定的 DefaultListableBeanFactory。
	 * @param beanFactory 用于此上下文的 DefaultListableBeanFactory 实例
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		// 将传入的 DefaultListableBeanFactory 传递给父类 GenericApplicationContext
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，从给定的组件类派生 bean-definition，并自动刷新上下文。
	 * @param componentClasses 一个或多个组件类，例如 {@link Configuration @Configuration} 类
	 */
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		this();
		register(componentClasses);
		refresh();
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，扫描给定包中的组件，为这些组件注册 bean-definition，并自动刷新上下文。
	 * @param basePackages 要扫描组件类的包
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}


	/**
	 * Propagate the given custom {@code Environment} to the underlying
	 * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link AnnotationBeanNameGenerator}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * Set the {@link ScopeMetadataResolver} to use for registered component classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// AnnotationConfigRegistry 的实现
	//---------------------------------------------------------------------

	/**
	 * 注册一个或多个需要处理的 component
	 * <p>注意，必须调用 {@link #refresh()} 才能使上下文完全处理新类。
	 * @param componentClasses 一个或多个 component，例如 {@link Configuration @Configuration} 类
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		StartupStep registerComponentClass = getApplicationStartup().start("spring.context.component-classes.register")
				.tag("classes", () -> Arrays.toString(componentClasses));
		this.reader.register(componentClasses);
		registerComponentClass.end();
	}

	/**
	 * 在指定的基础包内执行扫描。
	 * <p>注意，必须调用 {@link #refresh()} 才能使上下文完全处理新类。
	 * @param basePackages 要扫描组件类的包
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")
				.tag("packages", () -> Arrays.toString(basePackages));
		this.scanner.scan(basePackages);
		scanPackages.end();
	}


	//---------------------------------------------------------------------
	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
