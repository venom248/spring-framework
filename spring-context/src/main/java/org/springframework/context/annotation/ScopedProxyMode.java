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

/**
 * Enumerates the various scoped-proxy options.
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * <a href="https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html#beans-factory-scopes-other-injection"
 * >Scoped Beans as Dependencies</a> section of the Spring reference documentation.
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

	/**
	 * 默认值通常等于 {@link #NO}，除非在组件扫描指令级别配置了不同的默认值。
	 */
	DEFAULT,

	/**
	 * 不创建作用域代理。
	 * <p>通常在单例模式下使用
	 * 非单例模式下，则应优先使用 {@link #INTERFACES} 或 {@link #TARGET_CLASS}。
	 */
	NO,

	/**
	 * 创建一个 JDK 动态代理，实现目标对象的 <i>所有</i> 接口。
	 */
	INTERFACES,

	/**
	 * 使用 CGLIB 创建一个类代理。
	 */
	TARGET_CLASS

}
