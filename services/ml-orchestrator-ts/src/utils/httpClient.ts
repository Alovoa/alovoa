export class HttpError extends Error {
  statusCode: number;
  body: string;

  constructor(message: string, statusCode: number, body: string) {
    super(message);
    this.statusCode = statusCode;
    this.body = body;
  }
}

export async function postJson<TResponse>(
  url: string,
  payload: unknown,
  timeoutMs: number,
): Promise<TResponse> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });

    const text = await response.text();
    if (!response.ok) {
      throw new HttpError(`HTTP ${response.status} for ${url}`, response.status, text);
    }

    if (!text) {
      return {} as TResponse;
    }
    return JSON.parse(text) as TResponse;
  } finally {
    clearTimeout(timer);
  }
}

export async function getJson<TResponse>(url: string, timeoutMs: number): Promise<TResponse> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      method: "GET",
      signal: controller.signal,
    });
    const text = await response.text();
    if (!response.ok) {
      throw new HttpError(`HTTP ${response.status} for ${url}`, response.status, text);
    }
    if (!text) {
      return {} as TResponse;
    }
    return JSON.parse(text) as TResponse;
  } finally {
    clearTimeout(timer);
  }
}
