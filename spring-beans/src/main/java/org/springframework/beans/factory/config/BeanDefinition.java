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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * BeanDefinition 描述一个 Bean 实例，包括属性值、构造函数参数值以及具体实现提供的更多信息。
 *
 * <p>这只是一个最小的接口：主要意图是允许 {@link BeanFactoryPostProcessor} 检查和修改属性值以及其他 Bean 元数据
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * 单例作用域: {@value}.
	 * <p>请注意，扩展的 Bean 工厂可能支持更多范围。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 原型作用域: {@value}.
	 * <p>请注意，扩展的 Bean 工厂可能支持更多范围。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 表示 BeanDefinition 是应用程序的主要部分的角色提示。
	 * 通常对应于用户定义的 bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 表明 {@code BeanDefinition} 是某个更大配置的一部分，通常是外部 {@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 * {@code SUPPORT} bean 在查看特定 {@link org.springframework.beans.factory.parsing.ComponentDefinition} 时被认为是重要的，
	 * 但在查看应用程序的整体配置时不重要。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示，表示 {@code BeanDefinition} 提供完全后台角色，对最终用户没有任何意义。
	 * 当注册完全是 {@link org.springframework.beans.factory.parsing.ComponentDefinition} 内部工作的 bean 时使用此提示。
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// Modifiable attributes

	/**
	 * 设置此 BeanDefinition 的父定义的名称，如果有的话。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此 BeanDefinition 的父定义的名称，如果有的话。
	 */
	@Nullable
	String getParentName();

	/**
	 * 设置此 BeanDefinition 的 bean 类名。
	 * <p>在 bean 工厂后处理期间，通常会替换原始类名为其解析变体。
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 返回此 BeanDefinition 的当前 bean 类名。
	 * <p>请注意，这不一定是运行时实际使用的类名，如果子定义覆盖/继承类名，则不是这样。
	 * 此外，这可能只是调用工厂方法的类，或者在工厂 bean 引用上调用方法的情况下甚至可能为空。
	 * 因此，不要将其视为运行时的定义 bean 类型，而只在单个 bean 定义级别用于解析目的。
	 * @see #getParentName
	 * @see #getFactoryBeanName
	 * @see #getFactoryMethodName
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 设置此 BeanDefinition 的作用域。
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此 BeanDefinition 的当前作用域，如果尚未知道，则返回 {@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * 设置此 BeanDefinition 是否应懒加载。
	 * <p>如果 {@code false}，则 bean 将在启动时由执行单例的 bean 工厂实例化。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回此 BeanDefinition 是否应懒加载。
	 * <p>只适用于单例 bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置此 BeanDefinition 的依赖项的名称。
	 * bean 工厂将确保首先初始化这些 bean。
	 * <p>请注意，通常通过 bean 属性或构造函数参数来表达依赖关系。
	 * 此属性仅适用于其他类型的依赖关系，如静态 (*ugh*) 或启动时的数据库准备。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此 BeanDefinition 的依赖项的名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置此 bean 是否可以自动装配。
	 * <p>请注意，此标志仅影响基于类型的自动装配。
	 * 它不会影响显式名称引用，即使指定的 bean 未标记为自动装配候选者，也会解析。
	 * 因此，按名称自动装配将注入一个 bean，如果名称匹配的话。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此 bean 是否可以自动装配。
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此 bean 是否应该被考虑为自动装配候选者。
	 * <p>如果 {@code false}，则不会考虑此 bean 作为自动装配候选者，即使它是唯一的候选者。
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此 bean 是否是主要自动装配候选者。
	 */
	boolean isPrimary();

	/**
	 * 设置此 bean 是否是一个回退的自动装配候选者。
	 * <p>如果所有 bean 都是回退的候选者，那么将选择剩余的 bean。
	 * @since 6.2
	 * @see #setPrimary
	 */
	void setFallback(boolean fallback);

	/**
	 * 返回此 bean 是否是一个回退的自动装配候选者。
	 * @since 6.2
	 */
	boolean isFallback();

	/**
	 * 指定工厂 bean，如果有的话。这是调用指定工厂方法的 bean 的名称。
	 * <p>工厂 bean 名称仅对于基于实例的工厂方法是必需的。
	 * 对于静态工厂方法，该方法将从bean类派生。
	 * @see #setFactoryMethodName
	 * @see #setBeanClassName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂 bean 名称，如果有的话。
	 * <p>对于将从 bean 类派生的静态工厂方法，这将是 {@code null}。
	 * @see #getFactoryMethodName
	 * @see #getBeanClassName
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * 指定工厂方法，如果有的话。
	 * 该方法将使用构造函数参数调用，如果未指定参数，则不使用任何参数。
	 * 该方法将在指定的工厂 bean（如果有）上调用，或者作为本地 bean 类上的静态方法调用。
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回工厂方法，如果有的话。
	 * @see #getFactoryBeanName()
	 * @see #getBeanClassName()
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 返回此 Bean 的构造函数参数值。
	 * <p>返回的实例可以在 bean 工厂后处理期间进行修改。
	 * @return MutablePropertyValues 对象（永不为 {@code null}）
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 返回是否为此 bean 定义定义了构造函数参数值。
	 * @since 5.0.2
	 * @see #getConstructorArgumentValues()
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * 返回此 Bean 的属性值，以应用于 bean 的新实例。
	 * <p>返回的实例可以在 bean 工厂后处理期间进行修改。
	 * @return MutablePropertyValues 对象（永不为 {@code null}）
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * 返回是否为此 bean 定义定义了属性值。
	 * @since 5.0.2
	 * @see #getPropertyValues()
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * 指定初始化方法，如果有的话。
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * 返回初始化方法的名称，如果有的话。
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * 指定销毁方法的名称，如果有的话。
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * 返回销毁方法的名称，如果有的话。
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * 设置此 BeanDefinition 的角色提示。
	 * 角色提示提供框架以及工具关于特定 BeanDefinition 的角色和重要性的指示。
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * 获取此 BeanDefinition 的角色提示。
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * 设置一个人类可读的描述此 BeanDefinition。
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * 返回此 BeanDefinition 的人类可读描述，如果有的话。
	 */
	@Nullable
	String getDescription();


	// Read-only attributes

	/**
	 * 基于此 BeanDefinition 的 bean 类的名称，返回一个 ResolvableType。
	 * <p>请注意，这可能是一个 {@link ResolvableType#NONE}，如果 bean 类名是一个动态值，无法解析为具体的类。
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	ResolvableType getResolvableType();

	/**
	 * 返回是否是单例，即是否是共享的 bean 实例。
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * 返回是否是原型，即每次调用都会创建一个新的 bean 实例。
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * 返回是否是抽象的，即不应该实例化它本身，而只是作为具体子 bean 定义的父级。
	 */
	boolean isAbstract();

	/**
	 * 返回此 Bean 定义来源的描述（用于显示错误上下文）。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 返回原始 Bean 定义，如果有的话。
	 * <p>允许检索装饰的 BeanDefinition，如果有的话。
	 * <p>请注意，此方法返回最直接的起源。迭代起源链以找到用户定义的原始 BeanDefinition。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
