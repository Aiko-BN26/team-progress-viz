"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export type ToggleProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  pressed?: boolean;
  onToggle?: (value: boolean) => void;
  darkModeToggle?: boolean;
};

const Toggle = React.forwardRef<HTMLButtonElement, ToggleProps>(
  ({ className, pressed: pressedProp, onToggle,darkModeToggle, ...props }, ref) => {
    const [pressed, setPressed] = React.useState(pressedProp ?? false);

    const handleClick = () => {
      const newValue = !pressed;
      setPressed(newValue);
      onToggle?.(newValue);

      if(darkModeToggle){
        if (newValue) {
          // global.cssの.dark適用
          document.documentElement.classList.add("dark");
        }else{
          // global.cssの.dark解除
          document.documentElement.classList.remove("dark");
        }
      }
    };

    return (
      <button
        ref={ref}
        type="button"
        aria-pressed={pressed}
        onClick={handleClick}
        className={cn(
          "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
          pressed ? "bg-primary" : "bg-gray-300",
          className,
        )}
        {...props}
      >
        <span
          className={cn(
            "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
            pressed ? "translate-x-6" : "translate-x-1",
          )}
        />
      </button>
    );
  },
);

Toggle.displayName = "Toggle";

export { Toggle };