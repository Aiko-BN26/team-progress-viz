"use client";

import { AlertCircle } from "lucide-react";
import { useActionState } from "react";

import {
  syncOrganizationAction,
  type SyncOrganizationActionState,
} from "../actions";
import { SyncOrganizationButton } from "./sync-button";

const initialState: SyncOrganizationActionState = {
  ok: true,
};

export function SyncOrganizationForm({ organizationId }: { organizationId: string }) {
  const action = syncOrganizationAction.bind(null, organizationId);
  const [state, formAction] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-col items-end gap-2">
      <SyncOrganizationButton />
      {!state.ok && state.message ? (
        <p className="flex items-center gap-2 text-sm text-destructive">
          <AlertCircle className="h-4 w-4" />
          {state.message}
        </p>
      ) : null}
    </form>
  );
}
