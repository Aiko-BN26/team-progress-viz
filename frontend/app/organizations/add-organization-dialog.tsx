"use client";

import { FormEvent, useState, useTransition } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import type { RegisterOrganizationResult } from "./actions";

type AddOrganizationDialogProps = {
  registerOrganization: (input: {
    login: string;
  }) => Promise<RegisterOrganizationResult>;
};

export function AddOrganizationDialog({
  registerOrganization,
}: AddOrganizationDialogProps) {
  const [open, setOpen] = useState(false);
  const [login, setLogin] = useState("");
  const [status, setStatus] = useState<"idle" | "error">("idle");
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setLogin("");
      setStatus("idle");
      setStatusMessage(null);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    startTransition(async () => {
      const trimmedLogin = login.trim();
      if (!trimmedLogin) {
        setStatus("error");
        setStatusMessage("組織スラッグを入力してください");
        return;
      }

      const result = await registerOrganization({ login: trimmedLogin });
      if (result.ok) {
        handleOpenChange(false);
        return;
      }

      setStatus("error");
      setStatusMessage(
        result.message ?? "登録に失敗しました。時間をおいて再度お試しください。",
      );
    });
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button>Organizationを追加</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Organizationを追加</DialogTitle>
          <DialogDescription>
            GitHubのorganizationスラッグ（login）を入力してください。
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="organization-login">スラッグ名</Label>
            <Input
              id="organization-login"
              name="login"
              placeholder="例: your-team"
              autoComplete="off"
              value={login}
              onChange={(event) => setLogin(event.target.value)}
              disabled={isPending}
              autoFocus
            />
            {status === "error" && statusMessage && (
              <p className="text-sm text-destructive">{statusMessage}</p>
            )}
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={isPending}
            >
              キャンセル
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? "追加中..." : "追加"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
