package me.geso.avans;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.ToString;

/**
 * The response object using callback. This class is useful for streaming
 * response like CSV.
 */
@ToString
public class AvansCallbackResponse implements AvansResponse {

	private final Callback callback;
	private final AvansHeaders headers;

	public AvansCallbackResponse(@NonNull Callback callback) {
		this.callback = callback;
		this.headers = new AvansHeaders();
	}

	@Override
	public void write(HttpServletResponse response) throws IOException {
		headers.keySet().forEach(name -> {
			headers.getAll(name)
					.forEach(value -> response.addHeader(name, value));
		});
		try {
			this.callback.call(response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@FunctionalInterface
	public static interface Callback {
		public void call(HttpServletResponse resp) throws Exception;
	}

	@Override
	public void addHeader(String name, String value) {
		this.headers.add(name, value);
	}

	@Override
	public void setHeader(String name, String value) {
		this.headers.set(name, value);
	}
}
