package me.geso.avans;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class AvansRedirectResponse implements AvansResponse {

	private final String location;
	private final AvansHeaders headers;

	public AvansRedirectResponse(String location) {
		this.location = location;
		this.headers = new AvansHeaders();
	}

	@Override
	public void write(HttpServletResponse response) throws IOException {
		headers.keySet().forEach(name -> {
			headers.getAll(name)
					.forEach(value -> response.addHeader(name, value));
		});
		response.sendRedirect(location);
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
