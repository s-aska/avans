package me.geso.avans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.SneakyThrows;
import me.geso.avans.annotation.BodyParam;
import me.geso.avans.annotation.JsonParam;
import me.geso.avans.annotation.PathParam;
import me.geso.avans.annotation.QueryParam;

public class BasicAction implements Action {
	private final Class<? extends Controller> controllerClass;
	private final Method method;

	public BasicAction(Class<? extends Controller> klass,
			Method method) {
		this.controllerClass = klass;
		this.method = method;
	}

	public Method getMethod() {
		return method;
	}

	@SneakyThrows
	@Override
	public void invoke(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, Map<String, String> captured) {
		Controller controller = controllerClass.newInstance();
		controller.init(servletRequest, servletResponse, captured);
		WebResponse response = this.makeResponse(controller);
		controller.AFTER_DISPATCH(response);
		response.write(servletResponse);
	}

	@SneakyThrows
	private WebResponse makeResponse(Controller controller) {
		{
			Optional<WebResponse> maybeResponse = controller.BEFORE_DISPATCH();
			if (maybeResponse.isPresent()) {
				return maybeResponse.get();
			}
		}

		Object[] params = this.makeParameters(controller, method);
		WebResponse res = (WebResponse) method.invoke(controller, params);
		if (res == null) {
			throw new RuntimeException(
					"dispatch method must not return NULL");
		}
		return res;
	}

	private Object[] makeParameters(Controller controller, Method method) {
		Parameter[] parameters = method.getParameters();
		Class<?>[] types = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Object[] params = new Object[parameters.length];
		for (int i = 0; i < parameters.length; ++i) {
			params[i] = makeParameter(controller, method, parameters[i],
					types[i],
					parameterAnnotations[i]);
		}
		return params;
	}

	private Object makeParameter(Controller controller, Method method,
			Parameter parameter,
			Class<?> type, Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if (annotation instanceof JsonParam) {
				Object param = controller.getRequest().readJSON(type);
				return param;
			} else if (annotation instanceof QueryParam) {
				String name = ((QueryParam) annotation).value();
				return getObjectFromParameter(annotation, name, type,
						controller.getRequest()
								.getQueryParams());
			} else if (annotation instanceof BodyParam) {
				String name = ((BodyParam) annotation).value();
				return getObjectFromParameter(annotation, name, type,
						controller.getRequest()
								.getBodyParams());
			} else if (annotation instanceof PathParam) {
				String name = ((PathParam) annotation).value();
				return getObjectFromParameter(annotation, name, type,
						controller.getPathParameters());
			}
		}

		Object param = this.MAKE_PARAMETER(method, parameter);
		if (param == null) {
			throw new RuntimeException(String.format(
					"There is no way to create parameter: %s, %s, %s",
					controller.getClass().getName(), method.getName(),
					parameter.getName()));
		}
		return param;
	}

	/**
	 * Hook point for generating parameters.
	 * 
	 * @param method
	 * @param parameter
	 * @return
	 */
	protected Object MAKE_PARAMETER(Method method, Parameter parameter) {
		// I AM HOOK POINT
		return null;
	}

	/**
	 * You can override this method for extending type converter.
	 * 
	 * @param annotation
	 * @param name
	 * @param type
	 * @param params
	 * @return
	 */
	protected Object getObjectFromParameter(Annotation annotation, String name,
			Class<?> type,
			Parameters params) {
		if (type.equals(String.class)) {
			if (!params.containsKey(name)) {
				throw new RuntimeException(String.format(
						"Missing mandatory parameter '%s' by %s", name,
						annotation.getClass().getName()));
			}
			return params.get(name);
		} else if (type.equals(int.class)) {
			if (!params.containsKey(name)) {
				throw new RuntimeException(String.format(
						"Missing mandatory parameter '%s' by %s", name,
						annotation.getClass().getName()));
			}
			return params.getInt(name);
		} else if (type.equals(long.class)) {
			if (!params.containsKey(name)) {
				throw new RuntimeException(String.format(
						"Missing mandatory parameter '%s' by %s", name,
						annotation.getClass().getName()));
			}
			return params.getLong(name);
		} else if (type.equals(double.class)) {
			if (!params.containsKey(name)) {
				throw new RuntimeException(String.format(
						"Missing mandatory parameter '%s' by %s", name,
						annotation.getClass().getName()));
			}
			return params.getDouble(name);
		} else if (type.equals(OptionalInt.class)) {
			return params.getOptionalInt(name);
		} else if (type.equals(OptionalLong.class)) {
			return params.getOptionalLong(name);
		} else if (type.equals(OptionalDouble.class)) {
			return params.getOptionalDouble(name);
		} else if (type.equals(Optional.class)) {
			// avans supports Optional<String> only.
			return params.getOptional(name);
		} else {
			throw new RuntimeException(String.format(
					"Unknown parameter type '%s' for '%s'", type, name));
		}
	}

}
