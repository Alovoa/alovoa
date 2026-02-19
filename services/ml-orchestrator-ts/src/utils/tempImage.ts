import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { randomUUID } from "node:crypto";

export async function writeImageSourceToTempFile(source: {
  imageBase64?: string;
  imageUrl?: string;
}): Promise<string> {
  if (source.imageBase64) {
    const payload = source.imageBase64.includes(",")
      ? source.imageBase64.split(",", 2)[1]
      : source.imageBase64;
    const buffer = Buffer.from(payload, "base64");
    const file = path.join(os.tmpdir(), `aura-img-${randomUUID()}.jpg`);
    await fs.writeFile(file, buffer);
    return file;
  }

  if (source.imageUrl) {
    const response = await fetch(source.imageUrl);
    if (!response.ok) {
      throw new Error(`Failed to download image URL: ${source.imageUrl}; HTTP ${response.status}`);
    }
    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    const file = path.join(os.tmpdir(), `aura-img-${randomUUID()}.jpg`);
    await fs.writeFile(file, buffer);
    return file;
  }

  throw new Error("No image source provided");
}

export async function safeUnlink(filePath: string): Promise<void> {
  try {
    await fs.unlink(filePath);
  } catch {
    // ignore
  }
}
