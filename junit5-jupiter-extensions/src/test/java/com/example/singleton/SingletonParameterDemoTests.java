/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package com.example.singleton;

import com.example.singleton.ResourceParameterResolver.ClassResource;
import com.example.singleton.ResourceParameterResolver.GlobalResource;
import com.example.singleton.ResourceParameterResolver.MethodResource;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @since 5.4
 * @see ResourceParameterResolver
 */
@ExtendWith(ResourceParameterResolver.class)
class SingletonParameterDemoTests {

	private void log(String message, StringBuilder builder, TestInfo info) {
		String identity = "0x" + Integer.toHexString(System.identityHashCode(builder)).toUpperCase();
		System.out.println(identity + ":   " + message + " // " + info.getTestMethod().map(Method::getName).orElse("?"));
	}

	@Test
	void test1(@GlobalResource(Builder123.class) StringBuilder builder, TestInfo info) {
		log("GLOBAL" , builder, info);
	}

	@Test
	void test2(@GlobalResource(Builder123.class) Builder123 resource, TestInfo info) {
		log("GLOBAL", resource.getInstance(), info);
	}

	@Test
	void test3(@MethodResource(Builder123.class) StringBuilder new1, @MethodResource(Builder123.class) StringBuilder new2, TestInfo info) {
		log("METHOD", new1, info);
		log("METHOD", new2, info);
	}

	@Test
	void test4(@GlobalResource(value = Builder123.class, id = "T4") StringBuilder builder, TestInfo info) {
		log("T4    ", builder, info);
	}

	@Nested
	class M {

		@Test
		void m(@ClassResource(value = Builder123.class, id = "M") StringBuilder builder, TestInfo info) {
			log("M     ", builder, info);
		}

		@Nested
		class N {

			@Test
			void n1(@GlobalResource(value = Builder123.class, id = "M") Builder123 resource, TestInfo info) {
				log("M     ", resource.getInstance(), info);
			}

			@Test
			void n2(@GlobalResource(value = Builder123.class, id = "T4") Builder123 resource, TestInfo info) {
				log("T4    ", resource.getInstance(), info);
			}

			@Test
			void n3(@GlobalResource(Builder123.class) StringBuilder builder, TestInfo info) {
				log("GLOBAL", builder, info);
			}
		}
	}
}
