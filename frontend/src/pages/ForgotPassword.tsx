import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Shield, Eye, EyeOff, Loader2, AlertCircle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription, Input, Label } from '@/components/ui/primitives'
import { Button } from '@/components/ui/button'
import { authService } from '@/services/api'
import { toast } from 'sonner'

const schema = z.object({
  phone: z.string().regex(/^\d{10}$/, 'Enter 10-digit mobile number'),
  dateOfBirth: z.string().min(1, 'Date of birth is required'),
  securityQuestion: z.string().min(5, 'Security question is required').max(255, 'Max 255 characters'),
  securityAnswer: z.string().min(2, 'Security answer is required').max(255, 'Max 255 characters'),
  newPassword: z.string().min(6, 'Password must be at least 6 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type FormData = z.infer<typeof schema>

function normalizeIndianPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '').slice(0, 10)
  return `+91${digits}`
}

function getApiErrorMessage(error: any, fallback: string): string {
  const responseData = error?.response?.data
  if (typeof responseData?.message === 'string' && responseData.message.trim()) {
    return responseData.message
  }
  if (typeof responseData === 'string' && responseData.trim()) {
    return responseData
  }
  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message
  }
  return fallback
}

export default function ForgotPasswordPage() {
  const [showPw, setShowPw] = useState(false)
  const [showConfirmPw, setShowConfirmPw] = useState(false)
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      phone: '',
      dateOfBirth: '',
      securityQuestion: '',
      securityAnswer: '',
      newPassword: '',
      confirmPassword: '',
    },
  })

  const onSubmit = async (data: FormData) => {
    try {
      await authService.resetForgotPassword({
        phone: normalizeIndianPhone(data.phone),
        dateOfBirth: data.dateOfBirth,
        securityQuestion: data.securityQuestion,
        securityAnswer: data.securityAnswer,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword,
      })
      toast.success('Password reset successful. Please login with your new password.')
      navigate('/login')
    } catch (err: any) {
      toast.error(getApiErrorMessage(err, 'Password reset failed.'))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2 mb-4">
            <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center">
              <Shield className="h-6 w-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900">CrowdAid</span>
          </Link>
          <h1 className="text-2xl font-bold text-gray-900">Forgot Password</h1>
          <p className="text-gray-500 mt-1">Verify DOB and security question to reset your password.</p>
        </div>

        <Card className="shadow-medium">
          <CardHeader>
            <CardTitle>Reset Password</CardTitle>
            <CardDescription>Enter your registered details and set a new password.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="phone">Phone Number</Label>
                <div className="flex rounded-md border border-input bg-background">
                  <span className="inline-flex items-center px-3 text-sm text-gray-600 border-r border-input bg-gray-50 rounded-l-md">
                    +91
                  </span>
                  <Input
                    id="phone"
                    type="tel"
                    inputMode="numeric"
                    maxLength={10}
                    placeholder="9876543210"
                    className="border-0 focus-visible:ring-0 focus-visible:ring-offset-0 rounded-l-none"
                    {...register('phone', {
                      onChange: (e) => {
                        const digits = (e.target.value as string).replace(/\D/g, '').slice(0, 10)
                        setValue('phone', digits, { shouldValidate: true })
                      },
                    })}
                  />
                </div>
                {errors.phone && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.phone.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="dateOfBirth">Date of Birth</Label>
                <Input id="dateOfBirth" type="date" {...register('dateOfBirth')} />
                {errors.dateOfBirth && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.dateOfBirth.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="securityQuestion">Security Question</Label>
                <Input id="securityQuestion" placeholder="Enter your saved security question" {...register('securityQuestion')} />
                {errors.securityQuestion && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.securityQuestion.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="securityAnswer">Security Answer</Label>
                <Input id="securityAnswer" placeholder="Enter your saved security answer" {...register('securityAnswer')} />
                {errors.securityAnswer && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.securityAnswer.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="newPassword">New Password</Label>
                <div className="relative">
                  <Input
                    id="newPassword"
                    type={showPw ? 'text' : 'password'}
                    placeholder="Create a new password"
                    className="pr-10"
                    {...register('newPassword')}
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    onClick={() => setShowPw((prev) => !prev)}
                    tabIndex={-1}
                  >
                    {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.newPassword && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.newPassword.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <div className="relative">
                  <Input
                    id="confirmPassword"
                    type={showConfirmPw ? 'text' : 'password'}
                    placeholder="Confirm new password"
                    className="pr-10"
                    {...register('confirmPassword')}
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    onClick={() => setShowConfirmPw((prev) => !prev)}
                    tabIndex={-1}
                  >
                    {showConfirmPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className="text-xs text-red-500 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {errors.confirmPassword.message}
                  </p>
                )}
              </div>

              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Resetting...
                  </>
                ) : (
                  'Reset Password'
                )}
              </Button>
            </form>

            <p className="mt-5 text-center text-sm text-gray-500">
              Remembered your password?{' '}
              <Link to="/login" className="text-blue-600 hover:text-blue-500 font-medium">
                Back to login
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}