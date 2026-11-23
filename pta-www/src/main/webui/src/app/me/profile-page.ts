import { Component, computed, effect, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { problemDetail } from '../sessions/booking-actions.service';
import { Address } from './my-space.model';
import { MyProfile } from './my-space.service';

type Status = { kind: 'idle' } | { kind: 'saving' } | { kind: 'saved' } | { kind: 'error'; message: string };

/**
 * The participant edits their contact details and addresses, held by pta-customer.
 */
@Component({
  selector: 'pta-profile-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly space = inject(MyProfile);
  protected readonly me = inject(AuthService).me;
  protected readonly loaded = inject(AuthService).loaded;

  protected readonly profile = computed(() => (this.space.profile.hasValue() ? this.space.profile.value() : undefined));

  protected readonly contact = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^\s*\+?[0-9 ()-]{6,20}\s*$/)]],
  });

  protected readonly addresses = this.fb.group({
    delivery: this.address(true),
    separateBilling: [false],
    billing: this.address(false),
  });

  protected readonly contactStatus = signal<Status>({ kind: 'idle' });
  protected readonly addressStatus = signal<Status>({ kind: 'idle' });

  constructor() {
    // fill the forms once the profile is known
    effect(() => {
      const p = this.profile();
      if (!p) return;
      this.contact.reset({ email: p.email ?? '', phone: p.phone ?? '' });
      this.addresses.reset({
        delivery: blankIfNone(p.delivery),
        separateBilling: !!p.billing,
        billing: blankIfNone(p.billing),
      });
    });
  }

  private address(required: boolean) {
    const need = required ? [Validators.required, noComma] : [noComma];
    return this.fb.group({
      streetNo: ['', [noComma]],
      zip: ['', need],
      city: ['', need],
      region: ['', [noComma]],
      country: ['CH', need],
    });
  }

  protected async saveContact() {
    if (this.contact.invalid) {
      this.contact.markAllAsTouched();
      return;
    }
    const { email, phone } = this.contact.getRawValue();
    await this.save(this.contactStatus, () => this.space.saveContact(email.trim(), phone.trim()));
  }

  protected async saveAddresses() {
    const { delivery, separateBilling, billing } = this.addresses.getRawValue();
    const billingGroup = this.addresses.controls.billing;
    if (this.addresses.controls.delivery.invalid || (separateBilling && (billingGroup.invalid || !billing.zip || !billing.city))) {
      this.addresses.markAllAsTouched();
      return;
    }
    await this.save(this.addressStatus, () => this.space.saveAddresses(trimmed(delivery), separateBilling ? trimmed(billing) : undefined));
  }

  private async save(status: ReturnType<typeof signal<Status>>, action: () => Promise<void>) {
    status.set({ kind: 'saving' });
    try {
      await action();
      status.set({ kind: 'saved' });
    } catch (e) {
      status.set({ kind: 'error', message: problemDetail(e) });
    }
  }

  protected invalid(path: string): boolean {
    const control = this.contact.get(path) ?? this.addresses.get(path);
    return !!control && control.invalid && control.touched;
  }
}

function noComma(control: { value: unknown }) {
  return typeof control.value === 'string' && control.value.includes(',') ? { comma: true } : null;
}

function blankIfNone(address?: Address) {
  return {
    streetNo: address?.streetNo ?? '',
    zip: address?.zip ?? '',
    city: address?.city ?? '',
    region: address?.region ?? '',
    country: address?.country ?? 'CH',
  };
}

function trimmed(address: Required<Address>): Address {
  return Object.fromEntries(Object.entries(address).map(([k, v]) => [k, v.trim()])) as Address;
}
