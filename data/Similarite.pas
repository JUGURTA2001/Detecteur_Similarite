program Similarite;

{$mode objfpc}{$H+}

uses
  SysUtils, Math;

const
  MAX_TOKENS    = 4096;
  MAX_TOKEN_LEN = 64;

type
  TTokenArray = array[0..MAX_TOKENS-1] of string;

{ ---------- Utilitaire ---------- }

function Min3(a, b, c: Integer): Integer;
begin
  Result := a;
  if b < Result then Result := b;
  if c < Result then Result := c;
end;

function ReadFileAll(const Path: string): string;
var
  F: TFileStream;
  Buf: AnsiString;
begin
  Result := '';
  if not FileExists(Path) then Exit;
  F := TFileStream.Create(Path, fmOpenRead or fmShareDenyNone);
  try
    SetLength(Buf, F.Size);
    if F.Size > 0 then
      F.ReadBuffer(Buf[1], F.Size);
    Result := string(Buf);
  finally
    F.Free;
  end;
end;

{ ---------- Levenshtein ---------- }

function LevenshteinDistance(const S1, S2: string): Integer;
var
  N, M, I, J, Cost, Tmp: Integer;
  Prev, Cur: array of Integer;
begin
  N := Length(S1);
  M := Length(S2);
  SetLength(Prev, M + 1);
  SetLength(Cur,  M + 1);

  for J := 0 to M do Prev[J] := J;

  for I := 1 to N do
  begin
    Cur[0] := I;
    for J := 1 to M do
    begin
      if S1[I] = S2[J] then Cost := 0 else Cost := 1;
      Cur[J] := Min3(Prev[J] + 1,        { suppression }
                     Cur[J-1] + 1,        { insertion   }
                     Prev[J-1] + Cost);   { substitution }
    end;
    { échange Prev <-> Cur }
    for J := 0 to M do
    begin
      Tmp      := Prev[J];
      Prev[J]  := Cur[J];
      Cur[J]   := Tmp;
    end;
  end;

  Result := Prev[M];
end;

function LevenshteinSimilarity(const S1, S2: string): Double;
var
  N, M, MaxLen: Integer;
begin
  N := Length(S1);
  M := Length(S2);
  if (N = 0) and (M = 0) then Exit(1.0);
  MaxLen := Max(N, M);
  Result := 1.0 - LevenshteinDistance(S1, S2) / MaxLen;
end;

{ ---------- Tokenizer ---------- }

function IsAlnum(C: Char): Boolean;
begin
  Result := C in ['a'..'z', 'A'..'Z', '0'..'9', '_'];
end;

function Tokenize(const S: string; var Tokens: TTokenArray): Integer;
var
  I: Integer;
  Buf: string;
begin
  Result := 0;
  Buf := '';
  for I := 1 to Length(S) do
  begin
    if IsAlnum(S[I]) then
    begin
      if Length(Buf) < MAX_TOKEN_LEN - 1 then
        Buf := Buf + LowerCase(S[I]);
    end
    else if Buf <> '' then
    begin
      if Result >= MAX_TOKENS then Exit;
      Tokens[Result] := Buf;
      Inc(Result);
      Buf := '';
    end;
  end;
  if (Buf <> '') and (Result < MAX_TOKENS) then
  begin
    Tokens[Result] := Buf;
    Inc(Result);
  end;
end;

{ ---------- Jaccard ---------- }

function JaccardSimilarity(const S1, S2: string): Double;
var
  T1, T2: TTokenArray;
  N1, N2, I, J, Inter, Uni: Integer;
  Used: array[0..MAX_TOKENS-1] of Boolean;
begin
  N1 := Tokenize(S1, T1);
  N2 := Tokenize(S2, T2);

  if (N1 = 0) and (N2 = 0) then Exit(1.0);

  FillChar(Used, SizeOf(Used), 0);
  Inter := 0;

  for I := 0 to N1 - 1 do
    for J := 0 to N2 - 1 do
      if (not Used[J]) and (T1[I] = T2[J]) then
      begin
        Used[J] := True;
        Inc(Inter);
        Break;
      end;

  Uni := N1 + N2 - Inter;
  if Uni = 0 then Exit(0.0);
  Result := Inter / Uni;
end;

{ ---------- Cosinus ---------- }

function CosineSimilarity(const S1, S2: string): Double;
var
  F1, F2: array[0..255] of Integer;
  I, C: Integer;
  Dot, N1, N2: Double;
begin
  FillChar(F1, SizeOf(F1), 0);
  FillChar(F2, SizeOf(F2), 0);

  for I := 1 to Length(S1) do
  begin
    C := Ord(LowerCase(S1[I]));
    Inc(F1[C]);
  end;
  for I := 1 to Length(S2) do
  begin
    C := Ord(LowerCase(S2[I]));
    Inc(F2[C]);
  end;

  Dot := 0; N1 := 0; N2 := 0;
  for I := 0 to 255 do
  begin
    Dot := Dot + F1[I] * F2[I];
    N1  := N1  + F1[I] * F1[I];
    N2  := N2  + F2[I] * F2[I];
  end;

  if (N1 = 0) or (N2 = 0) then
  begin
    if (N1 = 0) and (N2 = 0) then Result := 1.0
    else Result := 0.0;
  end
  else
    Result := Dot / (Sqrt(N1) * Sqrt(N2));
end;

{ ---------- Plus longue sous-chaîne commune ---------- }

function LongestCommonSubstring(const S1, S2: string): Double;
var
  N, M, I, J, Best, Tmp: Integer;
  Prev, Cur: array of Integer;
begin
  N := Length(S1);
  M := Length(S2);
  if (N = 0) or (M = 0) then Exit(0.0);

  SetLength(Prev, M + 1);
  SetLength(Cur,  M + 1);
  for J := 0 to M do
  begin
    Prev[J] := 0;
    Cur[J]  := 0;
  end;

  Best := 0;
  for I := 1 to N do
  begin
    Cur[0] := 0;
    for J := 1 to M do
    begin
      if S1[I] = S2[J] then
      begin
        Cur[J] := Prev[J-1] + 1;
        if Cur[J] > Best then Best := Cur[J];
      end
      else
        Cur[J] := 0;
    end;
    { échange Prev <-> Cur }
    for J := 0 to M do
    begin
      Tmp     := Prev[J];
      Prev[J] := Cur[J];
      Cur[J]  := Tmp;
    end;
  end;

  Result := Best / Max(N, M);
end;

{ ---------- Score combiné ---------- }

function CombinedSimilarity(const S1, S2: string): Double;
var
  L, J, C, S: Double;
begin
  L := LevenshteinSimilarity(S1, S2);
  J := JaccardSimilarity(S1, S2);
  C := CosineSimilarity(S1, S2);
  S := LongestCommonSubstring(S1, S2);
  Result := 0.30 * L + 0.35 * J + 0.20 * C + 0.15 * S;
end;

{ ---------- Programme principal ---------- }

var
  S1, S2: string;
  Score: Double;
begin
  if ParamCount <> 2 then
  begin
    WriteLn('Usage: ', ParamStr(0), ' <fichier1> <fichier2>');
    Halt(1);
  end;

  S1 := ReadFileAll(ParamStr(1));
  S2 := ReadFileAll(ParamStr(2));

  if S1 = '' then
  begin
    WriteLn(StdErr, 'Impossible de lire : ', ParamStr(1));
    Halt(2);
  end;
  if S2 = '' then
  begin
    WriteLn(StdErr, 'Impossible de lire : ', ParamStr(2));
    Halt(2);
  end;

  WriteLn('Fichier 1  : ', ParamStr(1), ' (', Length(S1), ' octets)');
  WriteLn('Fichier 2  : ', ParamStr(2), ' (', Length(S2), ' octets)');
  WriteLn;

  WriteLn('Levenshtein         : ', LevenshteinSimilarity(S1, S2):0:4);
  WriteLn('Jaccard (mots)      : ', JaccardSimilarity(S1, S2):0:4);
  WriteLn('Cosinus (chars)     : ', CosineSimilarity(S1, S2):0:4);
  WriteLn('Sous-chaine commune : ', LongestCommonSubstring(S1, S2):0:4);
  WriteLn('----------------------------');

  Score := CombinedSimilarity(S1, S2);
  WriteLn('Score combine       : ', Score:0:4,
          '  (', (Score * 100):0:2, ' %)');
end.