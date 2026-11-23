import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ptaRequestHeader } from '../sessions/booking-actions.service';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ProfilePage>;

  const profile = {
    firstName: 'Alice', lastName: 'Martin', email: 'alice@example.ch', phone: '+41791234567',
    delivery: { streetNo: 'Rue du Lac 12', zip: '1003', city: 'Lausanne', region: 'VD', country: 'CH' },
  };

  async function render() {
    fixture = TestBed.createComponent(ProfilePage);
    fixture.detectChanges();
    http.expectOne('/api/me').flush({ authenticated: true, name: 'Alice', linked: true });
    await settle(fixture);
    http.expectOne('/api/me/profile').flush(profile);
    await settle(fixture);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const input = (page: HTMLElement, name: string, nth = 0) =>
    page.querySelectorAll<HTMLInputElement>(`input[formControlName="${name}"]`)[nth];

  function type(element: HTMLInputElement, value: string) {
    element.value = value;
    element.dispatchEvent(new Event('input'));
    element.dispatchEvent(new Event('blur'));
  }


  /** Lets pending promises and effects run, without waiting for open HTTP requests (unlike whenStable). */
  async function settle(f: ComponentFixture<unknown>) {
    await new Promise(resolve => setTimeout(resolve));
    TestBed.tick();
    f.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([ptaRequestHeader])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('is filled with the participant\'s current details', async () => {
    const page = await render();
    expect(input(page, 'email').value).toBe('alice@example.ch');
    expect(input(page, 'phone').value).toBe('+41791234567');
    expect(input(page, 'city').value).toBe('Lausanne');
    expect(page.querySelector('.note')?.textContent).toContain('sign-in e-mail is managed with your account');
  });

  it('saves the contact details', async () => {
    const page = await render();
    type(input(page, 'phone'), ' +41 79 000 00 00 ');
    (page.querySelector('form[aria-labelledby="contact-title"] button[type="submit"]') as HTMLButtonElement).click();

    const save = http.expectOne('/api/me/profile/contact');
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({ email: 'alice@example.ch', phone: '+41 79 000 00 00' });
    expect(save.request.headers.get('X-PTA-Request')).toBe('1');
    save.flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    http.expectOne('/api/me/profile').flush({ ...profile, phone: '+41790000000' });
    await settle(fixture);
    fixture.detectChanges();
    expect(page.querySelector('.status.is-saved')?.textContent).toBe('Saved');
  });

  it('does not send an invalid e-mail address', async () => {
    const page = await render();
    type(input(page, 'email'), 'not-an-address');
    (page.querySelector('form[aria-labelledby="contact-title"] button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(page.querySelector('.field-error')?.textContent).toBe('Please give a valid e-mail address.');
    http.expectNone('/api/me/profile/contact');
  });

  it('shows why the server refused the details', async () => {
    const page = await render();
    (page.querySelector('form[aria-labelledby="contact-title"] button[type="submit"]') as HTMLButtonElement).click();
    http.expectOne('/api/me/profile/contact').flush(
      { status: 400, detail: 'Please check the e-mail address and the phone number.' }, { status: 400, statusText: 'Bad Request' });
    await settle(fixture);
    fixture.detectChanges();
    expect(page.querySelector('.status.is-error')?.textContent).toBe('Please check the e-mail address and the phone number.');
  });

  it('saves the address, refusing commas the agency cannot store', async () => {
    const page = await render();
    const submit = page.querySelector('form[aria-labelledby="address-title"] button[type="submit"]') as HTMLButtonElement;

    type(input(page, 'streetNo'), 'Av. de Cour 1, 2nd floor');
    submit.click();
    fixture.detectChanges();
    http.expectNone('/api/me/profile/address');
    expect(page.textContent).toContain('without commas');

    type(input(page, 'streetNo'), 'Av. de Cour 1');
    type(input(page, 'zip'), '1007');
    submit.click();
    const save = http.expectOne('/api/me/profile/address');
    expect(save.request.body).toEqual({
      delivery: { streetNo: 'Av. de Cour 1', zip: '1007', city: 'Lausanne', region: 'VD', country: 'CH' },
      billing: null,
    });
    save.flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    http.expectOne('/api/me/profile').flush(profile);
  });
});
