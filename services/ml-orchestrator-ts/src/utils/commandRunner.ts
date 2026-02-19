import { exec } from "node:child_process";
import { promisify } from "node:util";

const execAsync = promisify(exec);

export interface CommandResult {
  code: number;
  stdout: string;
  stderr: string;
}

export async function runJsonCommand<TPayload>(
  commandTemplate: string,
  replacements: Record<string, string>,
  timeoutMs: number,
): Promise<TPayload> {
  let command = commandTemplate;
  for (const [key, value] of Object.entries(replacements)) {
    command = command.replaceAll(`{${key}}`, shellEscape(value));
  }

  const { stdout, stderr } = await execAsync(command, {
    timeout: timeoutMs,
    maxBuffer: 10 * 1024 * 1024,
    shell: "/bin/sh",
  });

  const trimmed = (stdout || "").trim();
  if (!trimmed) {
    throw new Error(`Command returned empty stdout: ${command}; stderr=${stderr}`);
  }

  try {
    return JSON.parse(trimmed) as TPayload;
  } catch (error) {
    throw new Error(`Invalid JSON from command: ${command}; stdout=${trimmed}; stderr=${stderr}; error=${String(error)}`);
  }
}

export function shellEscape(value: string): string {
  const safe = value.replaceAll("'", "'\\''");
  return `'${safe}'`;
}
